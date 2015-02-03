package org.langzhaozhi.dat;

/**
 * <p>一个基于DoubleArrayTrie实现的以String为Key的只读型Map,就是DoubleArrayTrie
 * 的Map化封装，get(aKey)的调用等价于调用DoubleArrayTrie的exactMatchSearch()，
 * 只是看起来Map化而已,比普通的java.util包下的各个Map(如HashMap、TreeMap)等快太多了,空间占用也小的多了</p>
 * <p>不变对象，意味着一旦构造就不再改变，因此可以任意多线程并发访问。</p>
 * <p>由于是不变对象，只适合于静态构造用途，不能适应于需要动态修改访问的环境。但对于预先确定的静态数据，
 * DoubleArrayTrieMap提供了相对的低内存占用、极速访问速度、绝对的并发访问支持</p>
 * <p>示例：</p>
 * <pre>
 *     StringPair<Something> [] pairArray = ....;
 *     DoubleArrayTrieMap<Something> datMap = DoubleArrayTrieMaker.makeMap( pairArray );
 *     ....
 *     Something v = datMap.get( "someThing" );
 * </pre>
 */
public final class DoubleArrayTrieMap<T> {
    private DoubleArrayTrie<T> mOwnerDat;

    DoubleArrayTrieMap(DoubleArrayTrie<T> aOwnerDat) {
        this.mOwnerDat = aOwnerDat;
    }

    public T get(CharSequence aKey) {
        return this.mOwnerDat.exactMatch( aKey );
    }

    /**
     * 转换成DAT调用方式
     */
    public DoubleArrayTrie<T> asDoubleArrayTrie() {
        return this.mOwnerDat;
    }
}
