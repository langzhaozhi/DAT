package org.langzhaozhi.dat;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.langzhaozhi.util.StringPair;

public class TestSerializeDeserialize {
    public static void main(String [] arge) throws Throwable {
        //词汇和其带的词性数组简单测试
        String testText = "山茶   {n=0}\n" + "干粮  {n=4}\n" + "隐身术 {n=0}\n" + "隐隐约约    {z=1}\n" + "男儿  {n=4}\n" + "隐蔽性 {n=3}\n" + "强记  {v=1}\n" + "来信  {n=35,v=15,vn=2}\n" + "荣列  {v=0}\n" + "登程  {v=0}\n" + "隔三差五    {l=1}\n" + "误字  {n=0}\n" + "小指  {n=0}\n" + "山草  {n=0}\n" + "牧草  {n=3}\n" + "铜版纸 {n=0}\n" + "突起  {v=1,vn=0}\n" + "醋意  {n=0}\n" + "让步  {vn=4,v=2}\n" + "狼藉  {a=0}\n" + "隔墙有耳    {i=0}\n" + "话剧票 {n=0}\n" + "隔声板 {n=0}\n" + "诊脉  {v=0}\n" + "探问  {v=0}";
        String [] testWordAndInfos = testText.split( "\n" );
        @SuppressWarnings("unchecked")
        StringPair<String []> [] pairs = Arrays.stream( testWordAndInfos ).map( (aWordAndInfo) -> {
            String [] parts = Arrays.stream( aWordAndInfo.split( "\\s" ) ).filter( (a) -> a.length() > 0 ).toArray( String []::new );

            String key = parts[ 0 ];
            String info = parts[ 1 ].substring( 1, parts[ 1 ].length() - 1 );//skip {}
            return new StringPair<String []>( key, info.split( "," ) );
        } ).toArray( StringPair []::new );

        long t1 = System.currentTimeMillis();
        DoubleArrayTrie<String []> dat = DoubleArrayTrieMaker.makeDoubleArrayTrie( pairs );
        long t2 = System.currentTimeMillis();
        System.err.println( "创建dat spend time: " + (t2 - t1) + " ms" );

        File datFile = new File( "e:/tmp/testdat.bin" );
        //把dat序列化到文件
        TestSerializeDeserialize.testSerialize( dat, datFile );
        //从文件反序列化并对比上面结果
        TestSerializeDeserialize.testDeserialize( datFile );
    }

    public static void testSerialize(DoubleArrayTrie<String []> aDat, File aDatFile) throws Throwable {
        Charset utf8 = Charset.forName( "UTF-8" );
        DoubleArrayTrieMaker.serializeDoubleArrayTrieToFile( aDat, aDatFile, (aValue) -> {
            ByteBuffer buf = ByteBuffer.allocate( 100 );
            buf.put( ( byte )aValue.length );
            Arrays.stream( aValue ).forEach( (aInfo) -> {
                ByteBuffer natureBuf = utf8.encode( aInfo );
                buf.put( ( byte )natureBuf.remaining() );
                buf.put( natureBuf );
            } );
            buf.flip();
            return buf;
        } );
    }

    public static void testDeserialize(File aDatFile) throws Throwable {
        Charset utf8 = Charset.forName( "UTF-8" );
        DoubleArrayTrie<String []> dat = DoubleArrayTrieMaker.deserializeDoubleArrayTrieFromFile( aDatFile, (aByteBuffer) -> {
            int natureCount = aByteBuffer.get() & 0xFF;
            String [] natureStrs = new String [ natureCount ];
            int oldLimit = aByteBuffer.limit();
            for (int i = 0; i < natureCount; ++i) {
                int charCount = aByteBuffer.get() & 0xFF;
                int pos = aByteBuffer.position();
                aByteBuffer.limit( pos + charCount ).position();
                natureStrs[ i ] = utf8.decode( aByteBuffer ).toString();
                aByteBuffer.limit( oldLimit ).position( pos + charCount );
            }
            return natureStrs;
        } );

        dat.forEachFast( true, (aText, aStart, aEnd, aValue) -> {
            System.err.println( "结果对比:" + aText + ",-----" + Arrays.asList( aValue ) );
            return true;
        } );
    }
}
