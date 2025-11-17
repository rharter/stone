package com.dropbox.stone;

import com.dropbox.stone.basic.Person;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class StructTest {
    @Test public void testRequiredBuilder() {
        Person person = new Person.Builder("Bill", 23L).build();
        assertThat(person).isNotNull();
    }

    @Test public void testCompleteBuilder() {
        Person person = new Person.Builder("Bill", 23L, "willy").build();
        assertThat(person).isNotNull();
    }

    @Test public void testCopyBuilder() {
        Person person = new Person.Builder("Bill", 23L).build();
        Person copy = new Person.Builder(person).build();
        assertThat(copy).isEqualTo(person);
    }
}
