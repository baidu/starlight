# [DynamicCompositeByteBuf](https://github.com/baidu/brpc-java/blob/master/brpc-java-core/src/main/java/com/baidu/brpc/buffer/DynamicCompositeByteBuf.java)
## 概述
DynamicCompositeByteBuf用来在读socket数据时，处理网络粘包/拆包问题，并能做到零拷贝。
其内部由多个netty ByteBuf组成，每次socket可读时，会append一个ByteBuf到DynamicCompositeByteBuf中；
然后从DynamicCompositeByteBuf读取一个完整的包，并移除不再使用的ByteBuf。

## 为什么不直接使用netty提供的CompositeByteBuf？
* CompositeByteBuf对引用计数不太灵活（为了兼容ByteBuf接口），每一次调用retainedSlice/readRetainedSlice，
都会增加CompositeByteBuf的引用计数，而不是增加内部ByteBuf引用计数。
* DynamicCompositeByteBuf是直接增加内部ByteBuf引用计数，本身并没有全局引用计数。
* CompositeByteBuf不会实时删除已读的ByteBuf，DynamicCompositeByteBuf会。

## 主要功能及设计
### 内部数据结构
* ArrayDeque，元素是netty ByteBuf。当有新buffer时，会加入队列；当从DynamicCompositeByteBuf读取数据后，会从队列中移除。
* readableBytes，所有ByteBuf的可读字节数。

## addBuffer
将ByteBuf直接加入队列，并增加readableBytes。
ByteBuf的引用计数并没有增加，其生命周期由DynamicCompositeByteBuf管理。

## retainedSlice
从DynamicCompositeByteBuf中截取length个字节，并返回一个netty CompositeByteBuf。
该函数会遍历队列中的ByteBuf，对ByteBuf进行retainedSlice并加入CompositeByteBuf，直到readableBytes达到length为止。
因此队列中返回的ByteBuf的refCnt会加1，且并不会出队，也不会改变DynamicCompositeByteBuf的readableBytes大小。
该功能主要用于读取请求包的header部分，遍历所有协议，尝试解析协议header部分，并判断出是哪个协议。

## skipBytes
移除length个字节对应的ByteBuf，并release调移除的ByteBuf。
该函数主要用于把请求的header长度从DynamicCompositeByteBuf移除。

## readRetainedSlice
从DynamicCompositeByteBuf中截取length个字节，并移除对应的ByteBuf。
如果某个ByteBuf完全被返回，则会从DynamicCompositeByteBuf中移除，并不增加ByteBuf的refCnt。
如果某个ByteBuf部分被返回，则会调用ByteBuf的readRetainedSlice，增加ByteBuf引用计数和readerIndex，
着是ByteBuf并不会从DynamicCompositeByteBuf移除，而是被两边同时引用。
该函数主要用于把请求的body部分的buffer截取出来，由后续的工作线程来解析并处理。
