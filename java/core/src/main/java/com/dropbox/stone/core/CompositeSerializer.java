package com.dropbox.stone.core;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public abstract class CompositeSerializer<T> extends StoneSerializer<T> {
    protected static final String TAG_FIELD = ".tag";

    protected static boolean hasTag(JsonParser p) throws IOException {
        return p.currentToken() == JsonToken.FIELD_NAME && TAG_FIELD.equals(p.currentName());
    }

    protected static String readTag(JsonParser p) throws IOException {
        if (!hasTag(p)) {
            return null;
        }
        p.nextToken();
        String tag = getStringValue(p);
        p.nextToken();

        return tag;
    }

    protected void writeTag(String tag, JsonGenerator g) throws IOException {
        if (tag != null) {
            g.writeStringField(TAG_FIELD, tag);
        }
    }
}

