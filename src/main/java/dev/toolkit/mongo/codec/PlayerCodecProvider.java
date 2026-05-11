package dev.toolkit.mongo.codec;

import dev.toolkit.mongo.model.PlayerProfile;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

/**
 * @author revqz
 */
public class PlayerCodecProvider implements CodecProvider {

    @Override
    @SuppressWarnings("unchecked")
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        if (clazz == PlayerProfile.class) {
            return (Codec<T>) new PlayerProfileCodec();
        }
        return null;
    }
}
