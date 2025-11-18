package com.dropbox.core.stone;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public abstract class StoneSerializer<T> implements IStoneSerializer<T> {
    private static final Charset UTF8 = StandardCharsets.UTF_8;

    @Override
    public String serialize(T value) {
        return serialize(value, false);
    }

    public String serialize(T value, boolean pretty) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            serialize(value, out, pretty);
        } catch (JsonGenerationException ex) {
            throw new IllegalStateException("Impossible JSON exception", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible I/O exception", ex);
        }
        return out.toString(UTF8);
    }

    @Override
    public void serialize(T value, OutputStream out) throws IOException {
        serialize(value, out, false);
    }

    public void serialize(T value, OutputStream out, boolean pretty) throws IOException {
        JsonGenerator g = Util.JSON.createGenerator(out);
        if (pretty) {
            g.useDefaultPrettyPrinter();
        }
        try {
            serialize(value, g);
        } catch (JsonGenerationException ex) {
            throw new IllegalStateException("Impossible JSON generation exception", ex);
        }
        g.flush();
    }

    @Override
    public T deserialize(String json) throws JsonParseException {
        try {
            JsonParser p = Util.JSON.createParser(json);
            p.nextToken();
            return deserialize(p);
        } catch (JsonParseException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new IllegalStateException("Impossible I/O exception", ex);
        }
    }

    @Override
    public T deserialize(InputStream json) throws IOException {
        JsonParser p = Util.JSON.createParser(json);
        p.nextToken();
        return deserialize(p);
    }

    public abstract void serialize(T value, JsonGenerator g) throws IOException;
    public abstract T deserialize(JsonParser p) throws IOException;

    protected static String getStringValue(JsonParser p) throws IOException {
        if (p.currentToken() != JsonToken.VALUE_STRING) {
            throw new JsonParseException(p, "expected string value, but was " + p.currentToken());
        }
        return p.getText();
    }

    protected static void expectField(String name, JsonParser p) throws IOException {
        if (p.currentToken() != JsonToken.FIELD_NAME) {
            throw new JsonParseException(p, "expected field name, but was: " + p.currentToken());
        }
        if (!name.equals(p.currentName())) {
            throw new JsonParseException(p, "expected field '" + name + "', but was: '" + p.currentName() + "'");
        }
        p.nextToken();
    }

    protected static void expectStartObject(JsonParser p) throws IOException {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            throw new JsonParseException(p, "expected object value.");
        }
        p.nextToken();
    }

    protected static void expectEndObject(JsonParser p) throws IOException {
        if (p.currentToken() != JsonToken.END_OBJECT) {
            throw new JsonParseException(p, "expected end of object value.");
        }
        p.nextToken();
    }

    protected static void expectStartArray(JsonParser p) throws IOException {
        if (p.currentToken() != JsonToken.START_ARRAY) {
            throw new JsonParseException(p, "expected array value.");
        }
        p.nextToken();
    }

    protected static void expectEndArray(JsonParser p) throws IOException {
        if (p.currentToken() != JsonToken.END_ARRAY) {
            throw new JsonParseException(p, "expected end of array value.");
        }
        p.nextToken();
    }

    protected static void skipValue(JsonParser p) throws IOException {
        if (p.currentToken().isStructStart()) {
            p.skipChildren(); // will leave parser at end token (e.g. '}' or ']')
            p.nextToken();
        } else if (p.currentToken().isScalarValue()) {
            p.nextToken();
        } else {
            throw new JsonParseException(p, "Can't skip JSON value token: " + p.currentToken());
        }
    }

    protected static void skipFields(JsonParser p) throws IOException {
        while (p.currentToken() != null && !p.currentToken().isStructEnd()) {
            if (p.currentToken().isStructStart()) {
                p.skipChildren();
                p.nextToken();
            } else if (p.currentToken() == JsonToken.FIELD_NAME) {
                p.nextToken();
            } else if (p.currentToken().isScalarValue()) {
                p.nextToken();
            } else {
                throw new JsonParseException(p, "Can't skip token: " + p.currentToken());
            }
        }
    }
}
