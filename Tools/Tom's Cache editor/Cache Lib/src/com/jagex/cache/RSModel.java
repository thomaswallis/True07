package com.jagex.cache;

import com.auradevil.t3d.data.Face;
import com.auradevil.t3d.data.Model;
import com.auradevil.t3d.data.Vertex;
import com.jagex.cache.util.ExtendedByteArrayOutputStream;
import com.jagex.cache.util.Stream;

import java.awt.*;
import java.awt.List;
import java.io.IOException;
import java.util.*;

/**
 * @author tom
 */
public class RSModel {
	private ModelValues currentModel;
	private static int[] colourMap;
	private static Map<Integer, Integer> map = new HashMap<Integer, Integer>();

	public RSModel() {
	}

	public static void init() {
		colourMap = new int[0x10000];
		(new Thread() {
			@Override
			public void run() {
				genColourMap();
			}
		}).start();
	}

	public static final void genColourMap() {
		int j = 0;
		for (int k = 0; k < 512; k++) {
			double d1 = (double) (k / 8) / 64D + 0.0078125D;
			double d2 = (double) (k & 7) / 8D + 0.0625D;
			for (int k1 = 0; k1 < 128; k1++) {
				double d3 = (double) k1 / 128D;
				double d4 = d3;
				double d5 = d3;
				double d6 = d3;
				if (d2 != 0.0D) {
					double d7;
					if (d3 < 0.5D)
						d7 = d3 * (1.0D + d2);
					else 
						d7 = (d3 + d2) - d3 * d2;
					double d8 = 2D * d3 - d7;
					double d9 = d1 + 0.33333333333333331D;
					if (d9 > 1.0D)
						d9--;
					double d10 = d1;
					double d11 = d1 - 0.33333333333333331D;
					if (d11 < 0.0D)
						d11++;
					if (6D * d9 < 1.0D)
						d4 = d8 + (d7 - d8) * 6D * d9;
					else if (2D * d9 < 1.0D)
						d4 = d7;
					else if (3D * d9 < 2D)
						d4 = d8 + (d7 - d8) * (0.66666666666666663D - d9) * 6D;
					else
						d4 = d8;
					if (6D * d10 < 1.0D)
						d5 = d8 + (d7 - d8) * 6D * d10;
					else if (2D * d10 < 1.0D)
						d5 = d7;
					else if (3D * d10 < 2D)
						d5 = d8 + (d7 - d8) * (0.66666666666666663D - d10) * 6D;
					else
						d5 = d8;
					if (6D * d11 < 1.0D)
						d6 = d8 + (d7 - d8) * 6D * d11;
					else if (2D * d11 < 1.0D)
						d6 = d7;
					else if (3D * d11 < 2D)
						d6 = d8 + (d7 - d8) * (0.66666666666666663D - d11) * 6D;
					else
						d6 = d8;
				}
				int l1 = (int) (d4 * 256D);
				int i2 = (int) (d5 * 256D);
				int j2 = (int) (d6 * 256D);
				int k2 = (l1 << 16) + (i2 << 8) + j2;
				colourMap[j] = k2;
				map.put(j++, k2);
			}
		}
		sortMap(map);
	}

	private static void sortMap(Map m) {
		ArrayList mapKeys = new ArrayList(m.keySet());
		ArrayList mapValues = new ArrayList(m.values());

		m.clear();

		TreeSet sortedSet = new TreeSet(mapValues);

		Object[] sortedArray = sortedSet.toArray();

		int size = sortedArray.length;
		for (int i = 0; i < size; i++) {
			m.put(mapKeys.get(mapValues.indexOf(sortedArray[i])), sortedArray[i]);
		}
	}

	public static int getColor(Color c) {
		int r = c.getRed();
		int g = c.getGreen();
		int b = c.getBlue();
		int rgbValue = (r << 16) + (g << 8) + (b);
		int current = 0;
		for (int i = 0; i < colourMap.length; i++) {
			if (Math.abs(colourMap[i] - rgbValue) < Math.abs(colourMap[current] - rgbValue)) {
				System.out.println("New current = "+colourMap[i]);
				current = i;
			}
		}
		System.out.println("Looking for color "+rgbValue+" found "+colourMap[current]+" "+current);
		return current;
	}

	public static int getColor0(Color c) {
		int r = c.getRed();
		int g = c.getGreen();
		int b = c.getBlue();
		int rgbValue = (r << 16) + (g << 8) + (b);
		int x = 0;
		int current = 0;
		Object[] vArr = map.values().toArray();
		while (rgbValue > current && x < vArr.length) {
			x++;
			current = (Integer)vArr[x];
		}
		System.out.println("Looking for color "+rgbValue+" found "+current+" "+x);
		return x;
	}

	public static Object getKeyFromValue(HashMap hm,Object value){
		for(Object o:hm.keySet()){
			if(hm.get(o).equals(value)) {
				return o;
			}
		}
		return null;
	}

	public void readHeader() {
		System.out.println("head");
		// Read header
		Stream header = new Stream(currentModel.modelData);
		header.caret = currentModel.modelData.length - 18;
		currentModel.numVertices = header.readUShort();
		currentModel.numTriangles = header.readUShort();
		currentModel.numTexTriangles = header.readUByte();
		int useTextures = header.readUByte();
		int useTriPriority = header.readUByte();
		int useTransparency = header.readUByte();
		int useTriSkinning = header.readUByte();
		int useVertSkinning = header.readUByte();
		int xDataLen = header.readUShort();
		int yDataLen = header.readUShort();
		int zDataLen = header.readUShort();
		int triDataLen = header.readUShort();

		// Find the data offsets
		int tmpOffset = 0;
		currentModel.vertexDirectionOffset = tmpOffset;
		tmpOffset += currentModel.numVertices;
		currentModel.triangleTypeOffset = tmpOffset;
		tmpOffset += currentModel.numTriangles;
		currentModel.trianglePriorityOffset = tmpOffset;
		if (useTriPriority == 255)
			tmpOffset += currentModel.numTriangles;
		else
			currentModel.trianglePriorityOffset = -useTriPriority - 1;
		currentModel.triangleSkinOffset = tmpOffset;
		if (useTriSkinning == 1)
			tmpOffset += currentModel.numTriangles;
		else
			currentModel.triangleSkinOffset = -1;
		currentModel.texturePointerOffset = tmpOffset;
		if (useTextures == 1)
			tmpOffset += currentModel.numTriangles;
		else
			currentModel.texturePointerOffset = -1;
		currentModel.vertexSkinOffset = tmpOffset;
		if (useVertSkinning == 1)
			tmpOffset += currentModel.numVertices;
		else
			currentModel.vertexSkinOffset = -1;
		currentModel.triangleAlphaOffset = tmpOffset;
		if (useTransparency == 1)
			tmpOffset += currentModel.numTriangles;
		else
			currentModel.triangleAlphaOffset = -1;
		currentModel.triDataOffset = tmpOffset;
		tmpOffset += triDataLen;
		currentModel.colourDataOffset = tmpOffset;
		tmpOffset += currentModel.numTriangles * 2;
		currentModel.uvMapTrianglesOffset = tmpOffset;
		tmpOffset += currentModel.numTexTriangles * 6;
		currentModel.xDataOffset = tmpOffset;
		tmpOffset += xDataLen;
		currentModel.yDataOffset = tmpOffset;
		tmpOffset += yDataLen;
		currentModel.zDataOffset = tmpOffset;
		tmpOffset += zDataLen;
	}

	public RSModel(byte[] modelData) {
		currentModel = new ModelValues();
		currentModel.modelData = modelData;
		readHeader();
		numVertices = currentModel.numVertices;
		numTriangles = currentModel.numTriangles;
		numTexTriangles = currentModel.numTexTriangles;
		verticesX = new int[numVertices];
		verticesY = new int[numVertices];
		verticesZ = new int[numVertices];
		trianglePoints1 = new int[numTriangles];
		trianglePoints2 = new int[numTriangles];
		trianglePoints3 = new int[numTriangles];
		texTrianglePointsX = new int[numTexTriangles];
		texTrianglePointsY = new int[numTexTriangles];
		texTrianglePointsZ = new int[numTexTriangles];
		if (currentModel.vertexSkinOffset >= 0)
			vertexSkins = new int[numVertices];
		if (currentModel.texturePointerOffset >= 0)
			texturePointers = new int[numTriangles];
		if (currentModel.trianglePriorityOffset >= 0)
			trianglePriorities = new int[numTriangles];
		if (currentModel.triangleAlphaOffset >= 0)
			triangleAlphaValues = new int[numTriangles];
		if (currentModel.triangleSkinOffset >= 0)
			triangleSkinValues = new int[numTriangles];
		colourValues = new int[numTriangles];
		Stream stream = new Stream(currentModel.modelData);
		stream.caret = currentModel.vertexDirectionOffset;
		Stream stream_1 = new Stream(currentModel.modelData);
		stream_1.caret = currentModel.xDataOffset;
		Stream stream_2 = new Stream(currentModel.modelData);
		stream_2.caret = currentModel.yDataOffset;
		Stream stream_3 = new Stream(currentModel.modelData);
		stream_3.caret = currentModel.zDataOffset;
		Stream stream_4 = new Stream(currentModel.modelData);
		stream_4.caret = currentModel.vertexSkinOffset;
		int baseOffsetX = 0;
		int baseOffsetY = 0;
		int baseOffsetZ = 0;
		for (int currentVertex = 0; currentVertex < numVertices; currentVertex++) {
			int flag = stream.readUByte();

			int currentOffsetX = 0;
			if ((flag & 1) != 0)
				currentOffsetX = stream_1.readSmart();

			int currentOffsetY = 0;
			if ((flag & 2) != 0)
				currentOffsetY = stream_2.readSmart();

			int currentOffsetZ = 0;
			if ((flag & 4) != 0)
				currentOffsetZ = stream_3.readSmart();

			verticesX[currentVertex] = baseOffsetX + currentOffsetX;
			verticesY[currentVertex] = baseOffsetY + currentOffsetY;
			verticesZ[currentVertex] = baseOffsetZ + currentOffsetZ;
			baseOffsetX = verticesX[currentVertex];
			baseOffsetY = verticesY[currentVertex];
			baseOffsetZ = verticesZ[currentVertex];
			if (vertexSkins != null)
				vertexSkins[currentVertex] = stream_4.readUByte();
		}

		stream.caret = currentModel.colourDataOffset;
		stream_1.caret = currentModel.texturePointerOffset;
		stream_2.caret = currentModel.trianglePriorityOffset;
		stream_3.caret = currentModel.triangleAlphaOffset;
		stream_4.caret = currentModel.triangleSkinOffset;
		for (int currentTriangle = 0; currentTriangle < numTriangles; currentTriangle++) {
			colourValues[currentTriangle] = stream.readUShort();
			if (texturePointers != null)
				texturePointers[currentTriangle] = stream_1.readUByte();
			if (trianglePriorities != null)
				trianglePriorities[currentTriangle] = stream_2.readUByte();
			if (triangleAlphaValues != null)
				triangleAlphaValues[currentTriangle] = stream_3.readUByte();
			if (triangleSkinValues != null)
				triangleSkinValues[currentTriangle] = stream_4.readUByte();
		}

		stream.caret = currentModel.triDataOffset;
		stream_1.caret = currentModel.triangleTypeOffset;
		int pointOffset1 = 0;
		int pointOffset2 = 0;
		int pointOffset3 = 0;
		int baseValue = 0;
		for (int currentTriangle = 0; currentTriangle < numTriangles; currentTriangle++) {
			int type = stream_1.readUByte();
			if (type == 1) {
				pointOffset1 = stream.readSmart() + baseValue;
				baseValue = pointOffset1;
				pointOffset2 = stream.readSmart() + baseValue;
				baseValue = pointOffset2;
				pointOffset3 = stream.readSmart() + baseValue;
				baseValue = pointOffset3;
				trianglePoints1[currentTriangle] = pointOffset1;
				trianglePoints2[currentTriangle] = pointOffset2;
				trianglePoints3[currentTriangle] = pointOffset3;
			}
			if (type == 2) {
				pointOffset1 = pointOffset1;
				pointOffset2 = pointOffset3;
				pointOffset3 = stream.readSmart() + baseValue;
				baseValue = pointOffset3;
				trianglePoints1[currentTriangle] = pointOffset1;
				trianglePoints2[currentTriangle] = pointOffset2;
				trianglePoints3[currentTriangle] = pointOffset3;
			}
			if (type == 3) {
				pointOffset1 = pointOffset3;
				pointOffset2 = pointOffset2;
				pointOffset3 = stream.readSmart() + baseValue;
				baseValue = pointOffset3;
				trianglePoints1[currentTriangle] = pointOffset1;
				trianglePoints2[currentTriangle] = pointOffset2;
				trianglePoints3[currentTriangle] = pointOffset3;
			}
			if (type == 4) {
				int origPointOffset1 = pointOffset1;
				pointOffset1 = pointOffset2;
				pointOffset2 = origPointOffset1;
				pointOffset3 = stream.readSmart() + baseValue;
				baseValue = pointOffset3;
				trianglePoints1[currentTriangle] = pointOffset1;
				trianglePoints2[currentTriangle] = pointOffset2;
				trianglePoints3[currentTriangle] = pointOffset3;
			}
		}

		stream.caret = currentModel.uvMapTrianglesOffset;
		for (int currentTexTriangle = 0; currentTexTriangle < numTexTriangles; currentTexTriangle++) {
			texTrianglePointsX[currentTexTriangle] = stream.readUShort();
			texTrianglePointsY[currentTexTriangle] = stream.readUShort();
			texTrianglePointsZ[currentTexTriangle] = stream.readUShort();
		}
	}

	public byte[] recompile() throws IOException {
		ExtendedByteArrayOutputStream finalBuf = new ExtendedByteArrayOutputStream();
		ExtendedByteArrayOutputStream vertexDirectionFlags = new ExtendedByteArrayOutputStream();
		ExtendedByteArrayOutputStream triangleType = new ExtendedByteArrayOutputStream();
		ExtendedByteArrayOutputStream triangleData = new ExtendedByteArrayOutputStream();
		ExtendedByteArrayOutputStream colourData = new ExtendedByteArrayOutputStream();
		ExtendedByteArrayOutputStream vertDataX = new ExtendedByteArrayOutputStream();
		ExtendedByteArrayOutputStream vertDataY = new ExtendedByteArrayOutputStream();
		ExtendedByteArrayOutputStream vertDataZ = new ExtendedByteArrayOutputStream();
		ExtendedByteArrayOutputStream footer = new ExtendedByteArrayOutputStream();

		int baseOffsetX = 0;
		int baseOffsetY = 0;
		int baseOffsetZ = 0;

		for (int i = 0; i < numVertices; i++) {
			int flag = 0;

			int vertX = verticesX[i];
			int vertY = -verticesY[i];
			int vertZ = -verticesZ[i];

			if (baseOffsetX != vertX) {
				int x = vertX - baseOffsetX;
				vertDataX.writeSmart(x);
				flag += 1;
				baseOffsetX = vertX;
			}
			if (baseOffsetY != vertY) {
				int y = vertY - baseOffsetY;
				vertDataY.writeSmart(y);
				flag += 2;
				baseOffsetY = vertY;
			}
			if (baseOffsetZ != vertZ) {
				int z = vertZ - baseOffsetZ;
				vertDataZ.writeSmart(z);
				flag += 4;
				baseOffsetZ = vertZ;
			}
			vertexDirectionFlags.write(flag);
		}

		int oldv1 = 0;
		int oldv2 = 0;
		int oldv3 = 0;
		int oldFaceType = 0;
		int[] bitarray = {1, 2, 4};
		int[] triangleDat = new int[numTriangles * 3];
		int[] faceType = new int[numTriangles];
		int offset = 0;
		int[] va = new int[3];

		for (int k = 0; k < numTriangles; k++) {
			int v1 = trianglePoints1[k];
			int v2 = trianglePoints2[k];
			int v3 = trianglePoints3[k];
			va[0] = v1;
			va[1] = v2;
			va[2] = v3;
			int vertCount = 0;
			int oldFlag = 0;
			int newflag = 0;

			for (int i = 0; i < 3; i++) {
				if (oldv1 == va[i]) {
					vertCount++;
					oldFlag++;
					newflag += bitarray[i];
				}
			}
			for (int i = 0; i < 3; i++) {
				if (oldv2 == va[i]) {
					vertCount++;
					oldFlag += 2;
					newflag += bitarray[i];
				}
			}
			for (int i = 0; i < 3; i++) {
				if (oldv3 == va[i]) {
					vertCount++;
					oldFlag += 4;
					newflag += bitarray[i];
				}
			}

			if (vertCount != 2) {
				faceType[k] = 1;
				for (int i = 0; i < 3; i++) {
					triangleDat[offset++] = va[i];
				}
				oldFaceType = 1;
				oldv1 = va[0];
				oldv2 = va[1];
				oldv3 = va[2];
			}

			int tv1, tv2, tv3;
			if (vertCount == 2) {
				if (oldFaceType == 1) {
					int oldVert = 7 - oldFlag;
					if (oldVert == 2) {
						tv1 = triangleDat[offset - 2];
						tv2 = triangleDat[offset - 1];
						tv3 = triangleDat[offset - 3];
						triangleDat[offset - 3] = tv1;
						oldv1 = tv1;
						triangleDat[offset - 2] = tv2;
						oldv2 = tv2;
						triangleDat[offset - 1] = tv3;
						oldv3 = tv3;
					}
					if (oldVert == 4) {
						tv1 = triangleDat[offset - 1];
						tv2 = triangleDat[offset - 3];
						tv3 = triangleDat[offset - 2];
						triangleDat[offset - 3] = tv1;
						oldv1 = tv1;
						triangleDat[offset - 2] = tv2;
						oldv2 = tv2;
						triangleDat[offset - 1] = tv3;
						oldv3 = tv3;
					}
				}

				int newVert = 7 - newflag;
				if (newVert == 1) {
					tv1 = va[1];
					tv2 = va[2];
					tv3 = va[0];
					va[0] = tv1;
					va[1] = tv2;
					va[2] = tv3;
				}
				if (newVert == 2) {
					tv1 = va[2];
					tv2 = va[0];
					tv3 = va[1];
					va[0] = tv1;
					va[1] = tv2;
					va[2] = tv3;
				}
				int triType = 6;
				if (oldv1 == va[0]) {
					triType = 2;
				}
				if (oldv2 == va[1]) {
					triType = 3;
				}

				boolean type6 = false;
				if (triType == 6) {
					type6 = true;
				}

				if (!type6) {
					faceType[k] = triType;
					oldFaceType = triType;
					triangleDat[offset++] = va[2];
					oldv1 = va[0];
					oldv2 = va[1];
					oldv3 = va[2];
				} else {
					faceType[k] = 1;
					for (int i = 0; i < 2; i++) {
						triangleDat[offset++] = va[i];
					}
					oldFaceType = 1;
					oldv1 = va[0];
					oldv2 = va[1];
					oldv3 = va[2];
				}
			}
		}

		int oldVertex = 0;
		for (int i = 0; i < offset; i++) {
			triangleData.writeSmart(triangleDat[i] - oldVertex);
			oldVertex = triangleDat[i];
		}

		for (int i = 0; i < numTriangles; i++) {
			triangleType.write(faceType[i]);
			colourData.writeShort(colourValues[i]);
		}

		footer.writeShort(numVertices);
		footer.writeShort(numTriangles);
		footer.write(numTexTriangles);
		footer.write(0); // Use textures FLAG
		footer.write(0); // Use Triangle Priorities FLAG (FF for True)
		footer.write(0); // Use Transparency FLAG
		footer.write(0); // Use Triangle Skinning FLAG
		footer.write(0); // Use Vertex Skinning FLAG
		footer.writeShort(vertDataX.size());
		footer.writeShort(vertDataY.size());
		footer.writeShort(vertDataZ.size());
		footer.writeShort(triangleData.size());

		vertexDirectionFlags.close();
		triangleType.close();
		triangleData.close();
		colourData.close();
		vertDataX.close();
		vertDataY.close();
		vertDataZ.close();
		footer.close();

		finalBuf.write(vertexDirectionFlags.toByteArray());
		finalBuf.write(triangleType.toByteArray());
		finalBuf.write(triangleData.toByteArray());
		finalBuf.write(colourData.toByteArray());
		finalBuf.write(vertDataX.toByteArray());
		finalBuf.write(vertDataY.toByteArray());
		finalBuf.write(vertDataZ.toByteArray());
		finalBuf.write(footer.toByteArray());
		finalBuf.close();
		return finalBuf.toByteArray();
	}

	public Model toModel() {
		Model m = new Model();
		Vector<Vertex> vertices = new Vector<Vertex>();
		Vector<Face> faces = new Vector<Face>();
		for (int i = 0; i < numVertices; i++) {
			vertices.add(new Vertex(verticesX[i], -verticesY[i], -verticesZ[i]));
		}
		for (int i = 0; i < numTriangles; i++) {
			faces.add(new Face(m, new Color(colourMap[colourValues[i]]), trianglePoints1[i], trianglePoints2[i], trianglePoints3[i]));
		}
		m.setVertices(vertices);
		m.setFaces(faces);
		return m;
	}

	public int numVertices;
	public int verticesX[];
	public int verticesY[];
	public int verticesZ[];
	public int numTriangles;
	public int trianglePoints1[];
	public int trianglePoints2[];
	public int trianglePoints3[];
	public int texturePointers[];
	public int trianglePriorities[];
	public int triangleAlphaValues[];
	public int colourValues[];
	public int numTexTriangles;
	public int texTrianglePointsX[];
	public int texTrianglePointsY[];
	public int texTrianglePointsZ[];
	public int vertexSkins[];
	public int triangleSkinValues[];
}
