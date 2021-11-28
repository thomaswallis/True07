package com.jagex.cache;

import com.jagex.cache.util.DataUtils;
import com.jagex.cache.util.ExtendedByteArrayOutputStream;

import java.util.ArrayList;
import java.io.IOException;

/**
 * @author tom
 */
public class ImageArchive {
    private final static String[] knownExceptions = {
            "index.dat", "title.dat"                    // A list of files in image archives which
                                                        // are known to be invalid. Used to filter un-necessary
                                                        // exceptions
    };
    private Archive jagArchive;
    private ArrayList<ImageGroup> images = new ArrayList<ImageGroup>();

    public ImageArchive(Archive jagArchive) {
        this.jagArchive = jagArchive;
        byte[] indexData = jagArchive.getFile("index.dat");
        for (int i = 0; i < jagArchive.getTotalFiles(); i++) {
            int hash = jagArchive.getIdentifierAt(i);
            if (validImage(hash)) {
                images.add(new ImageGroup(indexData, jagArchive.getFileAt(i), true));
            }
        }
    }

    public boolean validImage(int hash) {
        for (String s : knownExceptions) {
            if (hash == DataUtils.getHash(s)) {
                return false;
            }
        }
        return true;
    }

    public ImageGroup getImage(int i) {
        return images.get(i);
    }

    public void removeImage(int i) {
        images.remove(i);
        jagArchive.removeFile(i);
    }

    public void addImage(int hash, ImageGroup g) {
        images.add(0, g);
        jagArchive.addFileAt(0, hash, new byte[0]); // Add a placeholder
    }

    public int countImages() {
        return images.size();
    }

    public byte[] repackArchive() throws IOException {
        int x = 0;
        ExtendedByteArrayOutputStream indexBuf = new ExtendedByteArrayOutputStream();
        for (int i = 0; i < jagArchive.getTotalFiles(); i++) {
            int hash = jagArchive.getIdentifierAt(i);
            if (validImage(hash)) {
                images.get(x).appendThisIndex(indexBuf);
                jagArchive.updateFile(i, images.get(x++).packData());
            }
        }
        indexBuf.close();
        jagArchive.updateFile(jagArchive.indexOf("index.dat"), indexBuf.toByteArray());
        return jagArchive.recompile();
    }
}
