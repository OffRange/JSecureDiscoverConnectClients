package de.offrange.client.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Base64;

/**
 * Class that extends {@link TypeAdapter}. This is used to serialize byte arrays into a {@link Base64} string and
 * deserialize a base64 string into a byte[] object
 */
public class ByteArrayTypeAdapter extends TypeAdapter<byte[]> {

    @Override
    public void write(JsonWriter out, byte[] value) throws IOException {
        out.value(value == null ? null : Base64.getEncoder().encodeToString(value));
    }

    @Override
    public byte[] read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        return Base64.getDecoder().decode(in.nextString());
    }
}
