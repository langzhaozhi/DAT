package org.langzhaozhi.dat;

/**
 * 对参数化类型T的反序列化接口
 *
 * @param <T>
 */
@FunctionalInterface
public interface ValueDeserializer<T> {
    public T deserialize(CharSequence aData);
}
