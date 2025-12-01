package com.dropbox.stone.basic;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class UnionTest {
    @Test public void testStaticFactory() {
        Shape shape = Shape.circle(20.0);
        assertThat(shape).isNotNull();
    }

    @Test public void testIsVariant() {
        Shape shape = Shape.square(10.0);
        assertThat(shape.isSquare()).isTrue();
        assertThat(shape.isPoint()).isFalse();
    }

    @Test public void testIsOther() {
        Shape shape = Shape.OTHER;
        assertThat(shape.isOther()).isTrue();
    }

    @Test public void testGetter() {
        Shape shape = Shape.square(10.0);
        assertThat(shape.getSquare()).isEqualTo(10.0);
    }

    @Test public void testEqualsFailsWhenDifferent() {
        Shape square = Shape.square(10.0);
        assertThat(square).isNotEqualTo(Shape.circle(10.0));
    }

    @Test public void testEqualsPassesWhenSameValue() {
        Shape square = Shape.square(10.0);
        assertThat(square).isEqualTo(Shape.square(10.0));
    }

    @Test public void testEqualsPassesWhenSameInstance() {
        Shape square = Shape.square(10.0);
        assertThat(square).isEqualTo(square);
    }

    @Test public void testEqualsPassesForVoidTypes() {
        Shape point = Shape.point();
        assertThat(point).isEqualTo(Shape.point());
    }

    @Test public void testEqualsFailsForMismatchedVoidTypes() {
        Shape point = Shape.point();
        assertThat(point).isNotEqualTo(Shape.none());
    }

    @Test public void testToString() {
        assertThat(Shape.square(10.0).toString()).isNotNull();
    }
}
