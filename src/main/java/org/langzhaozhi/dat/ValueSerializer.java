package org.langzhaozhi.dat;

import java.nio.ByteBuffer;

/**
 * <p>对参数化类型T的序列化接口,用于对DAT进行结构和内容保存，以便下次加载的时候可以快速反序列化成DAT对象。
 * 由于在从原始数据构造生成DAT的时候，我这里采用的是尽可能压缩DAT数组长度的策略，因此生成DAT可能较慢(
 * 主要慢的地方在于不断检查base和check的冲突上)，不过一旦生成所占用的内存还是相对较少的。为了解决每次构造
 * DAT要花费大量时间，特别是数据量越大，构造DAT的时间就越慢。因此提供DAT序列化和反序列化的文件持久化方法是非常必要的：
 * 这个文件中保存了DAT的结构和内容，每次使用DAT的时候直接映象成生成好的DAT对象，达到超快速加载的目的。
 * 强烈推荐从原始数据构造DAT采用离线的方式：先生成DAT，再把生成好的文件持久化到文件中并在生产环境中静态使用。</p>
 * <p>为加快反序列化速度，采用二进制格式</p>
 *
 * @param <T>
 */
@FunctionalInterface
public interface ValueSerializer<T> {
    public ByteBuffer serialize(T aValue);
}
