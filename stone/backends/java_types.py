import argparse
import os.path
import string
from collections import OrderedDict
from typing import List, Set, Optional, Dict, Callable, Any, Iterable
from os import path

import jinja2

from stone.backend import CodeBackend
from stone.backends.helpers import split_words, fmt_camel
from stone.ir import Api, Struct, ApiNamespace, DataType, unwrap_nullable, is_user_defined_type, unwrap, UserDefined, \
    is_list_type, is_map_type, Field, is_nullable_type, is_struct_type, is_field_type, is_alias, StructField, \
    is_string_type, is_boolean_type, is_uint32_type, is_uint64_type, is_float32_type, is_float64_type, is_int32_type, \
    is_int64_type, is_bytes_type, is_union_type, is_integer_type, is_float_type, is_void_type, is_timestamp_type

_cmdline_parser = argparse.ArgumentParser(
    prog='java-types-backend',
)
_cmdline_parser.add_argument(
    '--package',
    type=str,
    required=True,
    help='Java package name of generated sources.',
)
_cmdline_parser.add_argument(
    '--annotation-package',
    type=str,
    default='jakarta.annotation',
    help='Package name containing Nonnull/Nullable annotations.',
)

class JavaTypesBackend(CodeBackend):

    cmdline_parser = _cmdline_parser

    def __init__(self, target_folder_path, args):
        super().__init__(target_folder_path, args)

        self.package: str = self.args.package
        self.nullable: str = f'{self.args.annotation_package}.Nullable'
        self.nonnull: str = f'{self.args.annotation_package}.Nonnull'

    def generate(self, api: Api) -> None:
        rsrc_folder = path.join(path.dirname(__file__), 'java_rsrc')

        template_loader = jinja2.FileSystemLoader(searchpath=rsrc_folder)
        template_env = jinja2.Environment(
            loader=template_loader,
            trim_blocks=True,
            lstrip_blocks=True,
        )

        template_globals = {
            'package': self.package,
            'nullable': self.nullable,
            'nonnull': self.nonnull,
            'imports': self.imports,
            'extends': self.extends,
            'type_name': fmt_type_name,
            'arg_name': self.fmt_arg_name,
            'getter_name': self.fmt_getter_name,
            'setter_name': self.fmt_setter_name,
            'method_name': self.fmt_method_name,
            'value': self.fmt_value,
            'javadoc': self.fmt_javadoc,
            'javadoc_line': self.fmt_javadoc_line,
            'has_default_value': self.has_default_value,
            'default_value': self.default_value,
            'nullable_annotation': self.nullable_annotation,
            'is_struct_type': is_struct_type,
            'is_union_type': is_union_type,
            'is_void_type': is_void_type,
            'is_nullable_type': is_nullable_type,
            'filter_field_named': filter_field_named,
            'non_void_fields': non_void_fields,
            'serializer': fmt_serializer,
        }

        base_path = path.join(self.target_folder_path, *self.package.split('.'))
        if not os.path.isdir(base_path):
            os.makedirs(base_path)

        for namespace in api.namespaces.values():
            target_dir = path.join(base_path, namespace.name)
            if not path.isdir(target_dir):
                os.makedirs(target_dir)

            for data_type in namespace.data_types:
                target_file: Optional[str] = None
                source: Optional[str] = None
                if is_struct_type(data_type):
                    target_file = path.join(target_dir, f'{fmt_type_name(data_type)}.java')
                    template = template_env.get_template(
                        name="Struct.java.jinja",
                        globals=template_globals,
                    )
                    source = template.render(
                        namespace=namespace,
                        type=data_type,
                    )
                elif is_union_type(data_type):
                    target_file = path.join(target_dir, f'{fmt_type_name(data_type)}.java')
                    template = template_env.get_template(
                        name="Union.java.jinja",
                        globals=template_globals,
                    )
                    source = template.render(
                        namespace=namespace,
                        type=data_type,
                    )

                if target_file is not None and source is not None:
                    with open(target_file, 'w', encoding='utf-8') as f:
                        f.write(source)

    def fmt_javadoc_line(
        self,
        docstring: str,
        prefix: str = ' * ',
        max_len: int = 80,
    ) -> str:
        lines: List[str] = []
        line = prefix
        line_len = max_len - len(prefix)
        for word in docstring.split():
            if len(line) + len(word) > line_len and len(line) > 0:
                lines.append(line)
                line = prefix

            if len(line) > len(prefix):
                line += ' '
            line += word

        return '\n'.join(lines)

    def fmt_javadoc(self, docstring: Optional[str]) -> str:
        if docstring is None or len(docstring) == 0:
            return ''
        elif len(docstring) < 73:
            return f'/** {docstring} */'
        else:
            return '\n'.join(['/**', *self.fmt_javadoc_line(docstring).splitlines(), ' */'])

    def fmt_arg_name(self, name: str) -> str:
        return fmt_camel(name)

    def fmt_getter_name(self, name: str) -> str:
        return self.fmt_method_name('get', name)

    def fmt_setter_name(self, name: str) -> str:
        return self.fmt_method_name('set', name)

    def fmt_method_name(self, *parts: str) -> str:
        words = []
        for part in parts:
            words.extend([w.capitalize() for w in split_words(part)])
        if len(words) > 0:
            words[0] = words[0].lower()
        return "".join(words)

    def fmt_fqcn(self, data_type: DataType) -> str:
        data_type, _, _ = unwrap(data_type)
        if is_user_defined_type(data_type):
            return f'{self.package}.{data_type.namespace.name}.{fmt_type_name(data_type)}'
        else:
            return ''

    def imports(
        self,
        data_type: DataType,
        namespace: ApiNamespace,
        additional_imports: Iterable[str] = None,
    ) -> Set[str]:
        """Returns a set of imports required for this data type"""
        if additional_imports is None:
            additional_imports = []
        imports: Set[str] = set(additional_imports)

        def add_import(dt: DataType):
            dt, nullable, _ = unwrap(dt)
            imports.add(self.nullable if nullable else self.nonnull)
            if is_user_defined_type(dt) and dt.namespace.name != namespace.name:
                imports.add(self.fmt_fqcn(dt))
            elif is_list_type(dt):
                imports.add('java.util.List')
                add_import(dt.data_type)
            elif is_map_type(dt):
                imports.add('java.util.Map')
                add_import(dt.key_data_type)
                add_import(dt.value_data_type)

        data_type = unwrap(data_type)[0]
        if is_user_defined_type(data_type):
            add_import(data_type)

            # Add Serializer support
            imports.add('com.dropbox.stone.core.StoneDeserializerLogger')
            imports.add('com.dropbox.stone.core.StoneSerializers')
            imports.add('com.fasterxml.jackson.core.JsonGenerator')
            imports.add('com.fasterxml.jackson.core.JsonParseException')
            imports.add('com.fasterxml.jackson.core.JsonParser')
            imports.add('com.fasterxml.jackson.core.JsonToken')
            if is_struct_type(data_type):
                imports.add('com.dropbox.stone.core.StructSerializer')
            elif is_union_type(data_type):
                imports.add('com.dropbox.stone.core.UnionSerializer')

            if data_type.parent_type is not None:
                add_import(data_type.parent_type)

            for field in data_type.all_fields:
                add_import(field.data_type)
        elif is_list_type(data_type):
            imports.add('java.util.List')
            add_import(data_type.data_type)
        elif is_map_type(data_type):
            imports.add('java.util.Map')
            add_import(data_type.key_data_type)
            add_import(data_type.value_data_type)

        return imports

    def extends(self, data_type: DataType) -> str:
        if is_user_defined_type(data_type) and data_type.parent_type is not None:
            return f' extends {fmt_type_name(data_type.parent_type)}'
        else:
            return ''

    def nullable_annotation(self, data_type: DataType) -> str:
        return "@Nullable" if _is_nullable(data_type) else "@Nonnull"

    def has_default_value(self, field: Field) -> bool:
        if not isinstance(field, StructField):
            return False
        return field.has_default or _is_nullable(field.data_type)

    def default_value(self, field: StructField) -> str:
        if field.has_default:
            return self.fmt_value(field.data_type, field.default)
        elif _is_nullable(field.data_type):
            return 'null'
        else:
            raise ValueError(f'StructField "{field.name}" doesn\'t have a default value')

    def fmt_value(self, data_type: DataType, value: str) -> str:
        if is_string_type(data_type):
            return f'"{value}"'
        elif is_boolean_type(data_type):
            return 'true' if value == 'True' else 'false'
        else:
            return value

def fmt_type_name(data_type: DataType) -> str:
    """Returns the Java type name for the resolved DataType"""
    data_type, _, _ = unwrap(data_type)

    # Built-in primitives should use Java types
    if is_uint32_type(data_type) or is_int32_type(data_type):
        return 'Integer'
    elif is_uint64_type(data_type) or is_int64_type(data_type):
        return 'Long'
    elif is_float32_type(data_type):
        return 'Float'
    elif is_float64_type(data_type):
        return 'Double'
    elif is_string_type(data_type):
        return 'String'
    elif is_boolean_type(data_type):
        return 'Boolean'
    elif is_bytes_type(data_type):
        return 'ByteArray'
    elif is_list_type(data_type):
        return f'List<{fmt_type_name(data_type.data_type)}>'
    elif is_map_type(data_type):
        return f'Map<{fmt_type_name(data_type.key_data_type)}, {fmt_type_name(data_type.value_data_type)}>'
    else:
        return ''.join([word.capitalize() for word in split_words(data_type.name)])

def fmt_serializer(data_type: DataType) -> str:
    print(f'Getting serializer for data type: {data_type.__class__}: {data_type}')
    while is_alias(data_type):
        data_type = data_type.data_type

    if is_nullable_type(data_type):
        if is_struct_type(data_type.data_type):
            return f'StoneSerializers.nullableStruct({fmt_serializer(data_type.data_type)})'
        else:
            return f'StoneSerializers.nullable({fmt_serializer(data_type.data_type)})'
    if is_list_type(data_type):
        return f'StoneSerializers.list({fmt_serializer(data_type.data_type)})'
    if is_map_type(data_type):
        return f'StoneSerializers.map({fmt_serializer(data_type.value_data_type)})'
    if is_user_defined_type(data_type):
        return f'{ fmt_type_name(data_type) }.Serializer.INSTANCE'
    if is_string_type(data_type):
        return 'StoneSerializers.string()'
    if is_bytes_type(data_type):
        return 'StoneSerializers.bytes()'
    if is_boolean_type(data_type):
        return 'StoneSerializers.boolean_()'
    if is_int32_type(data_type):
        return 'StoneSerializers.int32()'
    if is_int64_type(data_type):
        return 'StoneSerializers.int64()'
    if is_uint32_type(data_type):
        return 'StoneSerializers.uInt32()'
    if is_uint64_type(data_type):
        return 'StoneSerializers.uInt64()'
    if is_float32_type(data_type):
        return 'StoneSerializers.float32()'
    if is_float64_type(data_type):
        return 'StoneSerializers.float64()'
    if is_timestamp_type(data_type):
        return 'StoneSerializers.timestamp()'
    if is_void_type(data_type):
        return 'StoneSerializers.void_()'
    raise ValueError(f'No serializer for type: {data_type.__class__}: {data_type}')

def filter_field_named(fields: Iterable[Field], name: str) -> Iterable[Field]:
    return [f for f in fields if f.name != name]

def non_void_fields(fields: Iterable[Field]) -> Iterable[Field]:
    return [f for f in fields if not is_void_type(f.data_type)]

def _is_nullable(data_type: Any) -> bool:
    if is_nullable_type(data_type):
        return True
    elif is_alias(data_type):
        return _is_nullable(data_type.data_type)
    elif is_field_type(data_type):
        return _is_nullable(data_type.data_type)
    elif is_list_type(data_type):
        return _is_nullable(data_type.data_type)
    elif is_map_type(data_type):
        return _is_nullable(data_type.value_data_type)
    else:
        return False