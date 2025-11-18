package com.dropbox.core.stone;

import com.fasterxml.jackson.core.JsonParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface IStoneSerializer<T> {
    String serialize(T value);

    void serialize(T value, OutputStream out) throws IOException;

    T deserialize(String json) throws JsonParseException;

    T deserialize(InputStream json) throws IOException;
}
