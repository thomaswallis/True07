package com.jagex.cache.configloaders;

import com.jagex.cache.util.Stream;
import com.jagex.cache.util.ExtendedByteArrayOutputStream;
import com.jagex.cache.configloaders.beans.ItemBean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author tom
 */
public final class ItemConfig
{
    private static int[] streamIndices;
    private static ArrayList<ItemBean> items = new ArrayList<ItemBean>();
    private static Stream dataStream;

    public static void unpackConfig(byte[] data, byte[] index)
    {
        dataStream = new Stream(data);
        Stream indexStream = new Stream(index);
        int totalItems = indexStream.readUShort();
        streamIndices = new int[totalItems];
        int i = 2;
        for(int j = 0; j < totalItems; j++)
        {
            streamIndices[j] = i;
            i += indexStream.readUShort();
        }
        for (int x = 0; x < totalItems; x++) {
            items.add(forID(x));
        }
    }

    public static ItemBean forID(int i)
    {
        ItemBean itemDef = new ItemBean();
        dataStream.caret = streamIndices[i];
        readValues(itemDef, dataStream);
        return itemDef;
    }

    public static byte[][] recompile() throws IOException {
        ExtendedByteArrayOutputStream index = new ExtendedByteArrayOutputStream();
        ExtendedByteArrayOutputStream data = new ExtendedByteArrayOutputStream();
        index.writeShort(items.size());
		data.writeShort(items.size()); // Considering this is never read, WTF?
        for (ItemBean item : items) {
            byte[] newData = recompileData(item);
            data.write(newData);
            index.writeShort(newData.length);
        }
        index.close();
        data.close();
        return new byte[][] {index.toByteArray(), data.toByteArray()};
    }

    public static void addItem(ItemBean b) {
        items.add(b);
    }

    public static void removeItem(int i) {
        items.remove(i);
    }

	public static ItemBean getItem(int i) {
		return items.get(i);
	}

    private static byte[] recompileData(ItemBean b) throws IOException {
        ExtendedByteArrayOutputStream bos = new ExtendedByteArrayOutputStream();
        ItemBean defaults = new ItemBean();
        if (b.modelID != defaults.modelID) {
            bos.write(1);
            bos.writeShort(b.modelID);
        }
        if (b.name != null) {
            bos.write(2);
            bos.writeString(b.name);
        }
        if (b.description != null) {
            bos.write(3);
            bos.writeString(b.description);
        }
        if (b.modelInvZoom != defaults.modelInvZoom) {
            bos.write(4);
            bos.writeShort(b.modelInvZoom);
        }
        if (b.modelInvRotationX != defaults.modelInvRotationX) {
            bos.write(5);
            bos.writeShort(b.modelInvRotationX);
        }
        if (b.modelInvRotationY != defaults.modelInvRotationY) {
            bos.write(6);
            bos.writeShort(b.modelInvRotationY);
        }
        if (b.modelInvPosOffsetX != defaults.modelInvPosOffsetX) {
            bos.write(7);
            bos.writeShort(b.modelInvPosOffsetX);
        }
        if (b.modelInvPosOffsetY != defaults.modelInvPosOffsetY) {
            bos.write(8);
            bos.writeShort(b.modelInvPosOffsetY);
        }
        if (b.dummy != defaults.dummy) {
            bos.write(10);
            bos.writeShort(b.dummy);
        }
        if (b.stackable != defaults.stackable) {
            bos.write(11);
        }
        if (b.value != defaults.value) {
            bos.write(12);
            bos.writeInt(b.value);
        }
        if (b.membersObject != defaults.membersObject) {
            bos.write(16);
        }
        if (b.maleWornModelID != defaults.maleWornModelID || b.aByte205 != defaults.aByte205) {
            bos.write(23);
            bos.writeShort(b.maleWornModelID);
            bos.write(b.aByte205);
        }
        if (b.maleArmsID != defaults.maleArmsID) {
            bos.write(24);
            bos.writeShort(b.maleArmsID);
        }
        if (b.femaleWornModelID != defaults.femaleWornModelID || b.aByte154 != defaults.aByte154) {
            bos.write(25);
            bos.writeShort(b.femaleWornModelID);
            bos.write(b.aByte154);
        }
        if (b.femaleArmsID != defaults.femaleArmsID) {
            bos.write(26);
            bos.writeShort(b.femaleArmsID);
        }
        if (b.groundActions != defaults.groundActions) {
            for (int i = 0; i < 5; i++) {
                bos.write(30+i);
                if (b.groundActions[i] == null) {
                    bos.writeString("hidden");
                } else {
                    bos.writeString(b.groundActions[i]);
                }
            }
        }
        if (b.actions != defaults.actions) {
			int x = 0;
            for (int i = 0; i < 5; i++) {
				if (b.actions[i] != null) {
                	bos.write(35+(x++));
                	bos.writeString(b.actions[i]);
				}
            }
        }
        if (b.modifiedModelColors != defaults.modifiedModelColors || b.originalModelColors != defaults.originalModelColors) {
            bos.write(40);
            bos.write(b.modifiedModelColors.length);
            for (int i = 0; i < b.modifiedModelColors.length; i++) {
                bos.writeShort(b.modifiedModelColors[i]);
                bos.writeShort(b.originalModelColors[i]);
            }
        }
        if (b.anInt185 != defaults.anInt185) {
            bos.write(78);
            bos.writeShort(b.anInt185);
        }
        if (b.anInt162 != defaults.anInt162) {
            bos.write(79);
            bos.writeShort(b.anInt162);
        }
        if (b.anInt175 != defaults.anInt175) {
            bos.write(90);
            bos.writeShort(b.anInt175);
        }
        if (b.anInt197 != defaults.anInt197) {
            bos.write(91);
            bos.writeShort(b.anInt197);
        }
        if (b.anInt166 != defaults.anInt166) {
            bos.write(92);
            bos.writeShort(b.anInt166);
        }
        if (b.anInt173 != defaults.anInt173) {
            bos.write(93);
            bos.writeShort(b.anInt173);
        }
        if (b.anInt204 != defaults.anInt204) {
            bos.write(95);
            bos.writeShort(b.anInt204);
        }
        if (b.certID != defaults.certID) {
            bos.write(97);
            bos.writeShort(b.certID);
        }
        if (b.certTemplateID != defaults.certTemplateID) {
            bos.write(98);
            bos.writeShort(b.certTemplateID);
        }
        if (b.stackIDs != defaults.stackIDs || b.stackAmounts != defaults.stackAmounts) {
            for (int i = 0; i < 9; i++) {
                bos.write(i+100);
                bos.writeShort(b.stackIDs[i]);
                bos.writeShort(b.stackAmounts[i]);
            }
        }
        if (b.anInt167 != defaults.anInt167) {
            bos.write(110);
            bos.writeShort(b.anInt167);
        }
        if (b.anInt192 != defaults.anInt192) {
            bos.write(111);
            bos.writeShort(b.anInt192);
        }
        if (b.anInt191 != defaults.anInt191) {
            bos.write(112);
            bos.writeShort(b.anInt191);
        }
        if (b.anInt196 != defaults.anInt196) {
            bos.write(113);
            bos.write(b.anInt196);
        }
        if (b.anInt184 != defaults.anInt184) {
            bos.write(114);
            bos.write(b.anInt184);
        }
        if (b.team != defaults.team) {
            bos.write(115);
            bos.write(b.team);
        }
        bos.write(0);
        bos.close();
        return bos.toByteArray();
    }

    public static int countItems() {
        return items.size();
    }

    private static void readValues(ItemBean it, Stream stream)
    {
        do
        {
            int i = stream.readUByte();
            if(i == 0)
                return;
            if(i == 1)
                it.modelID = stream.readUShort();
            else
            if(i == 2)
                it.name = stream.readString();
            else
            if(i == 3)
                it.description = stream.readString();
            else
            if(i == 4)
                it.modelInvZoom = stream.readUShort();
            else
            if(i == 5)
                it.modelInvRotationX = stream.readUShort();
            else
            if(i == 6)
                it.modelInvRotationY = stream.readUShort();
            else
            if(i == 7)
            {
                it.modelInvPosOffsetX = stream.readUShort();
            } else
            if(i == 8)
            {
                it.modelInvPosOffsetY = stream.readUShort();
            } else
            if(i == 10)
                it.dummy = stream.readUShort();
            else
            if(i == 11)
                it.stackable = true;
            else
            if(i == 12)
                it.value = stream.readUInt();
            else
            if(i == 16)
                it.membersObject = true;
            else
            if(i == 23)
            {
                it.maleWornModelID = stream.readUShort();
                it.aByte205 = stream.readByte();
            } else
            if(i == 24)
                it.maleArmsID = stream.readUShort();
            else
            if(i == 25)
            {
                it.femaleWornModelID = stream.readUShort();
                it.aByte154 = stream.readByte();
            } else
            if(i == 26)
                it.femaleArmsID = stream.readUShort();
            else
            if(i >= 30 && i < 35)
            {
                if(it.groundActions == null)
                    it.groundActions = new String[5];
                it.groundActions[i - 30] = stream.readString();
            } else
            if(i >= 35 && i < 40)
            {
                if(it.actions == null)
                    it.actions = new String[5];
                it.actions[i - 35] = stream.readString();
            } else
            if(i == 40)
            {
                int j = stream.readUByte();
                it.modifiedModelColors = new int[j];
                it.originalModelColors = new int[j];
                for(int k = 0; k < j; k++)
                {
                    it.modifiedModelColors[k] = stream.readUShort();
                    it.originalModelColors[k] = stream.readUShort();
                }

            } else
            if(i == 78)
                it.anInt185 = stream.readUShort();
            else
            if(i == 79)
                it.anInt162 = stream.readUShort();
            else
            if(i == 90)
                it.anInt175 = stream.readUShort();
            else
            if(i == 91)
                it.anInt197 = stream.readUShort();
            else
            if(i == 92)
                it.anInt166 = stream.readUShort();
            else
            if(i == 93)
                it.anInt173 = stream.readUShort();
            else
            if(i == 95)
                it.anInt204 = stream.readUShort();
            else
            if(i == 97)
                it.certID = stream.readUShort();
            else
            if(i == 98)
                it.certTemplateID = stream.readUShort();
            else
            if(i >= 100 && i < 110)
            {
                if(it.stackIDs == null)
                {
                    it.stackIDs = new int[10];
                    it.stackAmounts = new int[10];
                }
                it.stackIDs[i - 100] = stream.readUShort();
                it.stackAmounts[i - 100] = stream.readUShort();
            } else
            if(i == 110)
                it.anInt167 = stream.readUShort();
            else
            if(i == 111)
                it.anInt192 = stream.readUShort();
            else
            if(i == 112)
                it.anInt191 = stream.readUShort();
            else
            if(i == 113)
                it.anInt196 = stream.readByte();
            else
            if(i == 114)
                it.anInt184 = stream.readByte();
            else
            if(i == 115)
                it.team = stream.readUByte();
        } while(true);
    }

}
