package com.dropbox.stone.basic;

import static com.google.common.truth.Truth.assertThat;

import com.fasterxml.jackson.core.FormatFeature;
import com.fasterxml.jackson.core.JsonParseException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class StructSerializerTest {
    @Test public void testGeneratesSerializer() {
        assertThat(Person.Serializer.INSTANCE).isNotNull();
    }

    @Test public void testSerializesSimpleType() {
        Person p = new Person.Builder("Foop", 14L)
                .setNickname("Foopy")
                .build();
        String result = Person.Serializer.INSTANCE.serialize(p);
        assertThat(result).isEqualTo("""
            {"name":"Foop","age":14,"nickname":"Foopy"}"""
        );
    }

    @Test public void testDeserializesSimpleType() throws JsonParseException {
        Person expected = new Person.Builder("Foop", 14L)
                .setNickname("Foopy")
                .build();

        Person result = Person.Serializer.INSTANCE.deserialize("""
                    {"name":"Foop","age":14,"nickname":"Foopy"}""");

        assertThat(result).isEqualTo(expected);
    }

    @Test public void testSerializesNestedStruct() throws JsonParseException {
        FancyPerson p = new FancyPerson.Builder(
                new Name.Builder("Charles", "Upton").build()
        ).build();
        String result = FancyPerson.Serializer.INSTANCE.serialize(p);
        assertThat(result).isEqualTo("""
                {"name":{"given_name":"Charles","surname":"Upton"}}""");
    }
}
