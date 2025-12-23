package memory.manager;

import memory.page.HeapPage;
import memory.page.Page;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class HeapPageFileManager implements PageFileManager {
    private final int PAGE_SIZE = 8192;

    @Override
    public void write(Page page, Path path) {
        if (page == null) {
            throw new IllegalArgumentException("page is null");
        }

        byte[] arr = page.bytes();
        if (arr == null || arr.length != PAGE_SIZE) {
            throw new IllegalArgumentException("invalid page bytes size");
        }

        try {
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);

            try (FileChannel ch = FileChannel.open(path,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE)) {

                long pos;
                if (page.getPageId() >= 0) {
                    pos = (long) page.getPageId() * PAGE_SIZE;
                } else {
                    pos = ch.size();
                }

                ch.position(pos);
                ByteBuffer buf = ByteBuffer.wrap(arr);
                while (buf.hasRemaining()) {
                    int n = ch.write(buf);
                    if (n <= 0) throw new IllegalStateException("short write");
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("I/O error while writing page", e);
        }
    }

    @Override
    public Page read(int pageId, Path path) {
        if (pageId < 0) {
            throw new IllegalArgumentException("invalid page id");
        }
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("page file does not exist");
        }

        try (FileChannel ch = FileChannel.open(path, StandardOpenOption.READ)) {
            ByteBuffer buf = ByteBuffer.allocate(PAGE_SIZE);
            long pos = (long) pageId * PAGE_SIZE;

            int total = 0;
            while (total < PAGE_SIZE) {
                int n = ch.read(buf, pos + total);
                if (n == -1) {
                    throw new IllegalArgumentException("page out of file (EOF)");
                }
                total += n;
            }

            byte[] arr = buf.array();
            return new HeapPage(pageId, arr);
        } catch (IOException e) {
            throw new IllegalStateException("I/O error while reading page", e);
        }
    }
}
