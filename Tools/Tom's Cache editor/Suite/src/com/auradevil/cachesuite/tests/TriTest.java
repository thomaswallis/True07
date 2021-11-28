package com.auradevil.cachesuite.tests;

import java.util.Arrays;

/**
 * @author tom
 */
public class TriTest {
	public static void main(String[] args) {
		System.out.println("Triangle packing test");
		int[][] origverts = new int[][]{
				{1, 0, 10},
				{10, 3, 2},
				{10, 4, 3},
				{10, 5, 4},
				{10, 6, 5},
				{10, 7, 6},
				{10, 8, 7},
				{10, 9, 8},
				{10, 0, 9},
				{11, 0, 1},
				{11, 1, 2},
				{11, 2, 3},
				{11, 3, 4},
				{11, 4, 5},
				{11, 5, 6},
				{11, 6, 7},
				{11, 7, 8},
				{11, 8, 9},
				{9, 0, 11}
		};

		System.out.println("Start tris: ");
		for (int i = 0; i < 19; i++) {
			System.out.println(Arrays.toString(origverts[i]));
		}

		int oldv1 = 0;
		int oldv2 = 0;
		int oldv3 = 0;
		int oldFaceType = 0;
		int[] bitarray = {1, 2, 4};
		int[] triDatBuffer = new int[19 * 3];
		int[] faceType = new int[19];
		int offset = 0;
		int[] va = new int[3];
		boolean type6 = false;
		int tv1, tv2, tv3;

		for (int k = 0; k < 19; k++) {
			type6 = false;
			int v1 = origverts[k][0];
			int v2 = origverts[k][1];
			int v3 = origverts[k][2];
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
					newflag |= bitarray[i];
				}
			}
			for (int i = 0; i < 3; i++) {
				if (oldv2 == va[i]) {
					vertCount++;
					oldFlag += 2;
					newflag |= bitarray[i];
				}
			}
			for (int i = 0; i < 3; i++) {
				if (oldv3 == va[i]) {
					vertCount++;
					oldFlag += 4;
					newflag |= bitarray[i];
				}
			}

			if (vertCount != 2) {
				faceType[k] = 1;
				for (int i = 0; i < 3; i++) {
					triDatBuffer[offset++] = va[i];
				}
				oldFaceType = 1;
				oldv1 = va[0];
				oldv2 = va[1];
				oldv3 = va[2];
			}

			if (vertCount == 2) {
				
				if (oldFaceType == 1) {
					int oldVert = 7 - oldFlag;
					if (oldVert == 2) {
						tv1 = triDatBuffer[offset - 2];
						tv2 = triDatBuffer[offset - 1];
						tv3 = triDatBuffer[offset - 3];
						triDatBuffer[offset - 3] = tv1;
						oldv1 = tv1;
						triDatBuffer[offset - 2] = tv2;
						oldv2 = tv2;
						triDatBuffer[offset - 1] = tv3;
						oldv3 = tv3;
					}
					if (oldVert == 4) {
						tv1 = triDatBuffer[offset - 1];
						tv2 = triDatBuffer[offset - 3];
						tv3 = triDatBuffer[offset - 2];
						triDatBuffer[offset - 3] = tv1;
						oldv1 = tv1;
						triDatBuffer[offset - 2] = tv2;
						oldv2 = tv2;
						triDatBuffer[offset - 1] = tv3;
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
				if (triType == 6) {
					type6 = true;
				}

				if (!type6) {
					faceType[k] = triType;
					oldFaceType = triType;
					triDatBuffer[offset++] = va[2];
					oldv1 = va[0];
					oldv2 = va[1];
					oldv3 = va[2];
				} else {
					faceType[k] = 1;
					oldFaceType = 1;
					for (int i = 0; i < 3; i++) {
						triDatBuffer[offset++] = va[i];
					}
					oldv1 = va[0];
					oldv2 = va[1];
					oldv3 = va[2];
				}
			}
		}

		int[] triDat = new int[offset];

		int oldVertex = 0;
		for (int i = 0; i < offset; i++) {
			triDat[i] = triDatBuffer[i] - oldVertex;
			oldVertex = triDatBuffer[i];
		}

		System.out.println("Made tri dat: " + Arrays.toString(triDat) + " and tri flags: " + Arrays.toString(faceType));


		int[][] newTris = new int[19][3];

		int pointOffset1 = 0;
		int pointOffset2 = 0;
		int pointOffset3 = 0;
		int baseValue = 0;
		int x = 0;
		for (int i = 0; i < 19; i++) {
			int type = faceType[i];
			if (type == 1) {
				pointOffset1 = triDat[x++] + baseValue;
				baseValue = pointOffset1;
				pointOffset2 = triDat[x++] + baseValue;
				baseValue = pointOffset2;
				pointOffset3 = triDat[x++] + baseValue;
				baseValue = pointOffset3;
				newTris[i][0] = pointOffset1;
				newTris[i][1] = pointOffset2;
				newTris[i][2] = pointOffset3;
			}
			if (type == 2) {
				pointOffset1 = pointOffset1;
				pointOffset2 = pointOffset3;
				pointOffset3 = triDat[x++] + baseValue;
				baseValue = pointOffset3;
				newTris[i][0] = pointOffset1;
				newTris[i][1] = pointOffset2;
				newTris[i][2] = pointOffset3;
			}
			if (type == 3) {
				pointOffset1 = pointOffset3;
				pointOffset2 = pointOffset2;
				pointOffset3 = triDat[x++] + baseValue;
				baseValue = pointOffset3;
				newTris[i][0] = pointOffset1;
				newTris[i][1] = pointOffset2;
				newTris[i][2] = pointOffset3;
			}
			if (type == 4) {
				int origPointOffset1 = pointOffset1;
				pointOffset1 = pointOffset2;
				pointOffset2 = origPointOffset1;
				pointOffset3 = triDat[x++] + baseValue;
				baseValue = pointOffset3;
				newTris[i][0] = pointOffset1;
				newTris[i][1] = pointOffset2;
				newTris[i][2] = pointOffset3;
			}
		}

		System.out.println("End tris: ");
		for (int i = 0; i < 19; i++) {
			System.out.println(Arrays.toString(newTris[i]));
		}
	}
}
