package com.jagex.cache;

import com.jagex.cache.util.ByteArray;
import com.jagex.cache.util.exceptions.CacheException;

import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author tom
 */
public class CacheIndice {

    private ArrayList<ByteArray> files = new ArrayList<ByteArray>();

    public CacheIndice(RandomAccessFile dataFile, RandomAccessFile indexFile, int cacheNo) {
        maxFileSize = 6500000;
        this.cacheNo = cacheNo;
        this.dataFile = dataFile;
        this.indexFile = indexFile;
        try {
            for (int i = 0; i < (indexFile.length()/6); i++) {
                byte[] data = null;
                try {
                    data = getFile(i);
                } catch (IOException ignored) {

                }
                if (data != null) {
                    files.add(new ByteArray(data));
                } else {
                    files.add(null);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getNumFiles() {
        return files.size();
    }

    public synchronized byte[] getFile(int fileNumber) throws IOException {
        seekTo(indexFile, fileNumber * 6);
        int readThisCycle;
        for(int totalRead = 0; totalRead < 6; totalRead += readThisCycle) // Read 6 bytes from index into readBuffer
        {
            readThisCycle = indexFile.read(readBuffer, totalRead, 6 - totalRead);
            if(readThisCycle == -1) {
                throw new CacheException("Error reading file");
            }

        }

        int totalFileSize = ((readBuffer[0] & 0xff) << 16) + ((readBuffer[1] & 0xff) << 8) + (readBuffer[2] & 0xff);
        int nextSectorId = ((readBuffer[3] & 0xff) << 16) + ((readBuffer[4] & 0xff) << 8) + (readBuffer[5] & 0xff);
        if(totalFileSize < 0 || totalFileSize > maxFileSize) {
             throw new CacheException("File size too large");
        }
        if(nextSectorId <= 0 || (long)nextSectorId > dataFile.length() / 520L) {
            throw new CacheException("Sector "+nextSectorId+" extends file bounds");
        }
        byte fileBuffer[] = new byte[totalFileSize];
        int dataReadSoFar = 0;
        int expectedFilePartitionNo = 0;
        while(totalFileSize > dataReadSoFar)
        {
            if(nextSectorId == 0) {
                throw new CacheException("Invalid sector id "+nextSectorId);
            }
            seekTo(dataFile, nextSectorId * 520); // 1 sector = 520 bytes
            int totalRead = 0;
            int amountToReadThisCycle = totalFileSize - dataReadSoFar;
            if(amountToReadThisCycle > 512)
                amountToReadThisCycle = 512;
            for(; totalRead < amountToReadThisCycle + 8; totalRead += readThisCycle) // Read the file + 8 bytes header
            {
                readThisCycle = dataFile.read(readBuffer, totalRead, (amountToReadThisCycle + 8) - totalRead);
                if(readThisCycle == -1)
                    throw new CacheException("Error reading file");
            }

            int nextSectorsFileNo = ((readBuffer[0] & 0xff) << 8) + (readBuffer[1] & 0xff);
            int filePartitionNo = ((readBuffer[2] & 0xff) << 8) + (readBuffer[3] & 0xff);
            int nextSectorsId = ((readBuffer[4] & 0xff) << 16) + ((readBuffer[5] & 0xff) << 8) + (readBuffer[6] & 0xff);
            int nextSectorsCacheNo = readBuffer[7] & 0xff;
            if(nextSectorsFileNo != fileNumber) {
                throw new CacheException("Sector file number didn't match expected file number");
            } else if(filePartitionNo != expectedFilePartitionNo) {
                throw new CacheException("Sector file part number didn't match expected file part number");
            } else if(nextSectorsCacheNo != cacheNo) {
                throw new CacheException("Sector cache number didn't match expected cache number");
            }
            if(nextSectorsId < 0 || (long)nextSectorsId > dataFile.length() / 520L) {
                throw new CacheException("Sector extends cache bounds!");
            }
            for(int copyCaret = 0; copyCaret < amountToReadThisCycle; copyCaret++)
                fileBuffer[dataReadSoFar++] = readBuffer[copyCaret + 8];

            nextSectorId = nextSectorsId;
            expectedFilePartitionNo++;
        }

        return fileBuffer;
    }

    public synchronized boolean addOrEditFile(int fileNo, int fileLength, byte fileBuffer[], boolean reportBoundsError) throws IOException {
        boolean fileExists = addFile(fileNo, fileLength, fileBuffer, true, reportBoundsError);
        if(!fileExists)
            fileExists = addFile(fileNo, fileLength, fileBuffer, false, reportBoundsError);
        return fileExists;
    }

    private synchronized boolean addFile(int fileNo, int fileLength, byte fileBuffer[], boolean fileExists, boolean reportBoundsError) throws IOException {
        int curSectorId;
        if(fileExists)
        {
            seekTo(indexFile, fileNo * 6);
            int readThisCycle;
            for(int i1 = 0; i1 < 6; i1 += readThisCycle) // Read the previous file index
            {
                readThisCycle = indexFile.read(readBuffer, i1, 6 - i1);
                if(readThisCycle == -1) {
                    return false;
                }
            }

            int lastFileSize = ((readBuffer[0] & 0xff) << 16) + ((readBuffer[1] & 0xff) << 8) + (readBuffer[2] & 0xff);
            curSectorId = ((readBuffer[3] & 0xff) << 16) + ((readBuffer[4] & 0xff) << 8) + (readBuffer[5] & 0xff);

            if ((fileLength/520) > (lastFileSize/520)) {
                if (reportBoundsError) {
                    updateFile(fileNo, fileBuffer);
                    throw new CacheException("New file sector count extends previous file bounds. Cache must be rebuilt");
                }
            }
            if(curSectorId <= 0 || (long)curSectorId > dataFile.length() / 520L) {
                //throw new CacheException("Sector extends cache bounds!");
            }
        } else
        {
            curSectorId = (int)((dataFile.length() + 519L) / 520L);
            if(curSectorId == 0)
                curSectorId = 1;
            addFile(fileBuffer);
        }
        readBuffer[0] = (byte)(fileLength >> 16);
        readBuffer[1] = (byte)(fileLength >> 8);
        readBuffer[2] = (byte)fileLength;
        readBuffer[3] = (byte)(curSectorId >> 16);
        readBuffer[4] = (byte)(curSectorId >> 8);
        readBuffer[5] = (byte)curSectorId;
        seekTo(indexFile, fileNo * 6);
        indexFile.write(readBuffer, 0, 6);
        int dataWrittenSoFar = 0;
        for(int expectedFilePartitionNo = 0; dataWrittenSoFar < fileLength; expectedFilePartitionNo++)
        {
            int nextSectorId = 0;
            if(fileExists)
            {
                seekTo(dataFile, curSectorId * 520);
                int j2;
                int l2;
                for(j2 = 0; j2 < 8; j2 += l2)
                {
                    l2 = dataFile.read(readBuffer, j2, 8 - j2);
                    if(l2 == -1)
                        break;
                }

                if(j2 == 8)
                {
                    int sectorFileNumber = ((readBuffer[0] & 0xff) << 8) + (readBuffer[1] & 0xff);
                    int prevFilePartitionNo = ((readBuffer[2] & 0xff) << 8) + (readBuffer[3] & 0xff);
                    nextSectorId = ((readBuffer[4] & 0xff) << 16) + ((readBuffer[5] & 0xff) << 8) + (readBuffer[6] & 0xff);
                    int prevCacheNo = readBuffer[7] & 0xff;
                    if(sectorFileNumber != fileNo)
                        throw new CacheException("Sector file number didn't match expected file number");
                    else if(prevFilePartitionNo != expectedFilePartitionNo)
                        throw new CacheException("Sector file part number didn't match expected file part number");
                    else if(prevCacheNo != cacheNo)
                        throw new CacheException("Sector cache number didn't match expected cache number");
                    if(nextSectorId < 0 || (long)nextSectorId > dataFile.length() / 520L)
                        throw new CacheException("Sector extends cache bounds!");
                }
            }
            if(nextSectorId == 0)
            {
                fileExists = false;
                nextSectorId = (int)((dataFile.length() + 519L) / 520L);
                if(nextSectorId == 0)
                    nextSectorId++;
                if(nextSectorId == curSectorId)
                    nextSectorId++;
            }
            if(fileLength - dataWrittenSoFar <= 512)
                nextSectorId = 0;
            readBuffer[0] = (byte)(fileNo >> 8);
            readBuffer[1] = (byte)fileNo;
            readBuffer[2] = (byte)(expectedFilePartitionNo >> 8);
            readBuffer[3] = (byte)expectedFilePartitionNo;
            readBuffer[4] = (byte)(nextSectorId >> 16);
            readBuffer[5] = (byte)(nextSectorId >> 8);
            readBuffer[6] = (byte)nextSectorId;
            readBuffer[7] = (byte) cacheNo;
            seekTo(dataFile, curSectorId * 520);
            dataFile.write(readBuffer, 0, 8);
            int amountOfDataWrittenThisCycle = fileLength - dataWrittenSoFar;
            if(amountOfDataWrittenThisCycle > 512)
                amountOfDataWrittenThisCycle = 512;
            dataFile.write(fileBuffer, dataWrittenSoFar, amountOfDataWrittenThisCycle);
            dataWrittenSoFar += amountOfDataWrittenThisCycle;
            curSectorId = nextSectorId;
        }

        return true;
    }

    public synchronized void seekTo(RandomAccessFile randomaccessfile, int position)
        throws IOException
    {
        if(position < 0 || position > 0x3c00000)
        {
            System.out.println("Badseek - pos:" + position + " len:" + randomaccessfile.length());
            position = 0x3c00000;
            try
            {
                Thread.sleep(1000L);
            }
            catch(Exception ignored) { }
        }
        randomaccessfile.seek(position);
    }

    public ArrayList<ByteArray> getFiles() {
        return files;
    }

    public void updateFile(int file, byte[] data) {
        files.set(file, new ByteArray(data));
    }

    public void addFile(byte[] data) {
        files.add(new ByteArray(data));
    }

    public void removeFile(int file) {
        files.remove(file);
    }

    static byte readBuffer[] = new byte[520];
    RandomAccessFile dataFile;
    RandomAccessFile indexFile;
    int cacheNo;
    int maxFileSize;
}
