package com.auradevil.cachesuite.util;

import com.auradevil.t3d.data.Model;
import com.auradevil.t3d.data.Vertex;
import com.auradevil.t3d.data.Face;
import com.jagex.cache.util.LEDataInputStream;
import com.jagex.cache.RSModel;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;
import java.awt.*;

/**
 * @author tom
 */
public class Model3DS {
	Vector<Vertex> vertices = new Vector<Vertex>();
	Vector<Face> faces = new Vector<Face>();
	Vector<Material3DS> materials = new Vector<Material3DS>();
	int curMat = 0;
	Model m = new Model();
	Color ambientTemp = Color.BLACK;
	Color diffuseTemp = Color.WHITE;
	int xx = 0;

	public Model3DS(byte[] data) throws IOException {
		LEDataInputStream dis = new LEDataInputStream(new ByteArrayInputStream(data));
		dis.setLittleEndianMode(true);
		while (dis.availiable() != 0) {
			readChunk(dis);
		}
	}

	private void readChunk(LEDataInputStream dis) throws IOException {
		int chunkID;
		int chunkLength;
		chunkID = dis.readUnsignedShort();
		chunkLength = dis.readInt();
		switch (chunkID) {
			case 0x02: // 3DS Version
				int ver = dis.readInt();
				readChunk(dis);
				break;
			case 0x4000: // Object
				// Read the object name
				String name = dis.readStrZ();
				readChunk(dis);
				break;
			case 0x4110: // Vertices list
				int numVertices = dis.readShort();
				for (int i = 0; i < numVertices; i++) {
					float vertX = dis.readFloat();
					float vertY = dis.readFloat();
					float vertZ = dis.readFloat();
					vertices.add(new Vertex(vertX, vertY, vertZ));
				}
				break;
			case 0x4120: // Faces list
				int numFaces = dis.readShort();
				System.out.println(numFaces);
				for (int i = 0; i < numFaces; i++) {
					int pointA = dis.readShort();
					int pointB = dis.readShort();
					int pointC = dis.readShort();
					int faceInfo = dis.readShort();
					faces.add(new Face(m, Color.RED, pointA, pointB, pointC));
				}
				break;
			case 0x4130:
				String matName = dis.readStrZ();
				int num = dis.readShort();
				Color thisColor = getColor(matName);
				for (int i = 0; i < num; i++) {
					int face = dis.readShort();
					System.out.println(matName+" "+face+" "+thisColor);
					faces.get(face).setFaceColor(thisColor);
				}
				break;
			case 0xA000: // Mat name
				Material3DS newMat = new Material3DS();
				newMat.setName(dis.readStrZ());
				System.out.println("new mat "+newMat.getName());
				materials.add(newMat);
				break;
			case 0xA010: // Mat ambient color
				ambientTemp = readColor(dis);
				break;
			case 0xA020: // Mat diffuse color
				diffuseTemp = readColor(dis);
				int tmp1 = ambientTemp.getRed();
				int tmp2 = diffuseTemp.getRed();
				int r, g, b;
				r = tmp1 + tmp2;
				tmp1 = ambientTemp.getGreen();
				tmp2 = diffuseTemp.getGreen();
				g = tmp1 + tmp2;
				tmp1 = ambientTemp.getBlue();
				tmp2 = diffuseTemp.getBlue();
				b = tmp1 + tmp2;
				if (r > 255) r = 255;
				if (g > 255) g = 255;
				if (b > 255) b = 255;
				System.out.println(r+" "+g+" "+b);
				materials.get(curMat++).setColour(new Color(r, g, b));
				break;
			case 0xAFFF: // Material editor
			case 0x4100: // Triangle mesh
			case 0x4D4D: // Main chunk
			case 0x3D3D: // 3D Editor
				readChunk(dis);
				break;
			default:
				dis.skipBytes(chunkLength-6); // chunk - header
				break;
		}
	}

	private Color getColor(String material) {
		for (Material3DS m : materials) {
			if (m.getName().equalsIgnoreCase(material)) {
				return m.getColour();
			}
		}
		return new Color(0xFF00FF);
	}

	private Color readColor(LEDataInputStream dis) throws IOException {
		int chunkID;
		int chunkLength;
		chunkID = dis.readUnsignedShort();
		chunkLength = dis.readInt();
		int r = 0xff;
		int g = 0x00;
		int b = 0xff;
		switch (chunkID) {
			case 0x0013:
			case 0x0010:
				float rf = dis.readFloat();
				float gf = dis.readFloat();
				float bf = dis.readFloat();
				r = (int) (rf*256);
				g = (int) (gf*256);
				b = (int) (bf*256);
				break;
			case 0x0011:
			case 0x0012:
				r = dis.readUnsignedByte();
				g = dis.readUnsignedByte();
				b = dis.readUnsignedByte();
				break;
			default:
				dis.skipBytes(chunkLength-6); // chunk - header
				break;
		}
		return new Color(r, g, b);
	}

	public RSModel toRSModel() {
		Model model = toModel();
		RSModel rsm = new RSModel();
		rsm.numVertices = model.getVertices().size();
		rsm.numTriangles = model.getFaces().size();
		System.out.println(rsm.numTriangles);
		rsm.verticesX = new int[rsm.numVertices];
		rsm.verticesY = new int[rsm.numVertices];
		rsm.verticesZ = new int[rsm.numVertices];

		rsm.trianglePoints1 = new int[rsm.numTriangles];
		rsm.trianglePoints2 = new int[rsm.numTriangles];
		rsm.trianglePoints3 = new int[rsm.numTriangles];

		rsm.colourValues = new int[rsm.numTriangles];

		for (int currentVertex = 0; currentVertex < rsm.numVertices; currentVertex++) {
			Vertex v = model.getVert(currentVertex);
			rsm.verticesX[currentVertex] = Math.round(v.getLocalPoint().getX());
			rsm.verticesY[currentVertex] = Math.round(v.getLocalPoint().getY());
			rsm.verticesZ[currentVertex] = Math.round(v.getLocalPoint().getZ());
		}

		for (int i = 0; i < rsm.numTriangles; i++) {
			Face f = model.getFace(i);
			rsm.trianglePoints1[i] = f.getPoints()[0];
			rsm.trianglePoints2[i] = f.getPoints()[1];
			rsm.trianglePoints3[i] = f.getPoints()[2];
			rsm.colourValues[i] = RSModel.getColor(f.getFaceColor());
		}
		return rsm;
	}

	public Model toModel() {
		m.setVertices(vertices);
		m.setFaces(faces);
		return m;
	}
}
