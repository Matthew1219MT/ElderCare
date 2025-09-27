package com.eldercare.eldercare.renderer;

import android.opengl.GLES20;
import android.util.Log;
import com.eldercare.eldercare.model.FaceScanData;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.List;

public class Face3DRenderer {
    private static final String TAG = "Face3DRenderer";

    // Vertex shader for face mesh
    private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "attribute vec3 vNormal;" +
                    "attribute float vConfidence;" +
                    "varying vec3 vColor;" +
                    "varying float vAlpha;" +
                    "varying vec3 vLightIntensity;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "  vec3 lightDirection = normalize(vec3(0.5, 0.8, 1.0));" +
                    "  float intensity = max(0.3, dot(vNormal, lightDirection));" +
                    "  vLightIntensity = vec3(intensity);" +
                    "  vColor = vec3(0.0, 0.9, 1.0);" +
                    "  vAlpha = vConfidence * 0.8;" +
                    "  gl_PointSize = 4.0;" +
                    "}";

    // Fragment shader for face mesh
    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "varying vec3 vColor;" +
                    "varying float vAlpha;" +
                    "varying vec3 vLightIntensity;" +
                    "void main() {" +
                    "  vec3 finalColor = vColor * vLightIntensity;" +
                    "  vec3 glowColor = finalColor + vec3(0.2, 0.3, 0.5);" +
                    "  gl_FragColor = vec4(glowColor, vAlpha);" +
                    "}";

    // Vertex shader for wireframe
    private final String wireframeVertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "}";

    // Fragment shader for wireframe
    private final String wireframeFragmentShaderCode =
            "precision mediump float;" +
                    "uniform float uTime;" +
                    "void main() {" +
                    "  float pulse = 0.7 + 0.3 * sin(uTime * 3.0);" +
                    "  gl_FragColor = vec4(0.0, 1.0, 1.0, 0.6 * pulse);" +
                    "}";

    // Vertex shader for points
    private final String pointVertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "attribute float vConfidence;" +
                    "varying float vAlpha;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "  vAlpha = vConfidence;" +
                    "  gl_PointSize = 3.0 + (vConfidence * 2.0);" +
                    "}";

    // Fragment shader for points
    private final String pointFragmentShaderCode =
            "precision mediump float;" +
                    "varying float vAlpha;" +
                    "uniform float uTime;" +
                    "void main() {" +
                    "  float pulse = 0.8 + 0.2 * sin(uTime * 5.0);" +
                    "  vec2 center = gl_PointCoord - vec2(0.5);" +
                    "  float dist = length(center);" +
                    "  if (dist > 0.5) discard;" +
                    "  float alpha = (1.0 - dist * 2.0) * vAlpha * pulse;" +
                    "  gl_FragColor = vec4(0.0, 1.0, 1.0, alpha);" +
                    "}";

    // Buffer objects
    private FloatBuffer vertexBuffer;
    private FloatBuffer normalBuffer;
    private FloatBuffer confidenceBuffer;
    private ShortBuffer indexBuffer;

    // Shader program handles
    private int meshProgram;
    private int wireframeProgram;
    private int pointProgram;

    // Mesh shader handles
    private int meshPositionHandle;
    private int meshNormalHandle;
    private int meshConfidenceHandle;
    private int meshMvpMatrixHandle;

    // Wireframe shader handles
    private int wireframePositionHandle;
    private int wireframeMvpMatrixHandle;
    private int wireframeTimeHandle;

    // Point shader handles
    private int pointPositionHandle;
    private int pointConfidenceHandle;
    private int pointMvpMatrixHandle;
    private int pointTimeHandle;

    // Render data
    private int vertexCount;
    private int triangleCount;
    private FaceScanData scanData;
    private boolean initialized = false;

    // Animation
    private long startTime;

    // Render modes
    public enum RenderMode {
        POINTS_ONLY,
        WIREFRAME_ONLY,
        MESH_ONLY,
        POINTS_AND_WIREFRAME,
        MESH_AND_WIREFRAME,
        ALL
    }

    private RenderMode renderMode = RenderMode.ALL;

    public Face3DRenderer(FaceScanData scanData) {
        this.scanData = scanData;
        this.startTime = System.currentTimeMillis();
        prepareMeshData();
    }

    public void setRenderMode(RenderMode mode) {
        this.renderMode = mode;
    }

    public void onSurfaceCreated() {
        try {
            // Create shader programs
            meshProgram = createShaderProgram(vertexShaderCode, fragmentShaderCode);
            wireframeProgram = createShaderProgram(wireframeVertexShaderCode, wireframeFragmentShaderCode);
            pointProgram = createShaderProgram(pointVertexShaderCode, pointFragmentShaderCode);

            if (meshProgram == 0 || wireframeProgram == 0 || pointProgram == 0) {
                Log.e(TAG, "Failed to create shader programs");
                return;
            }

            // Get mesh shader handles
            meshPositionHandle = GLES20.glGetAttribLocation(meshProgram, "vPosition");
            meshNormalHandle = GLES20.glGetAttribLocation(meshProgram, "vNormal");
            meshConfidenceHandle = GLES20.glGetAttribLocation(meshProgram, "vConfidence");
            meshMvpMatrixHandle = GLES20.glGetUniformLocation(meshProgram, "uMVPMatrix");

            // Get wireframe shader handles
            wireframePositionHandle = GLES20.glGetAttribLocation(wireframeProgram, "vPosition");
            wireframeMvpMatrixHandle = GLES20.glGetUniformLocation(wireframeProgram, "uMVPMatrix");
            wireframeTimeHandle = GLES20.glGetUniformLocation(wireframeProgram, "uTime");

            // Get point shader handles
            pointPositionHandle = GLES20.glGetAttribLocation(pointProgram, "vPosition");
            pointConfidenceHandle = GLES20.glGetAttribLocation(pointProgram, "vConfidence");
            pointMvpMatrixHandle = GLES20.glGetUniformLocation(pointProgram, "uMVPMatrix");
            pointTimeHandle = GLES20.glGetUniformLocation(pointProgram, "uTime");

            // Enable blending for transparency effects
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

            // Enable depth testing
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glDepthFunc(GLES20.GL_LEQUAL);

            initialized = true;
            Log.d(TAG, "Face3DRenderer initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error in onSurfaceCreated", e);
        }
    }

    private void prepareMeshData() {
        if (scanData == null || scanData.getPoints() == null) {
            Log.e(TAG, "No scan data available");
            return;
        }

        try {
            List<FaceScanData.FacePoint> points = scanData.getPoints();
            vertexCount = points.size();

            Log.d(TAG, "Preparing mesh data with " + vertexCount + " vertices");

            // Prepare vertex data
            float[] vertices = new float[vertexCount * 3];
            float[] normals = new float[vertexCount * 3];
            float[] confidences = new float[vertexCount];

            // Extract vertex data
            for (int i = 0; i < vertexCount; i++) {
                FaceScanData.FacePoint point = points.get(i);
                vertices[i * 3] = point.getX();
                vertices[i * 3 + 1] = point.getY();
                vertices[i * 3 + 2] = point.getZ();

                // Initialize normals (will be calculated later if triangles exist)
                normals[i * 3] = 0.0f;
                normals[i * 3 + 1] = 0.0f;
                normals[i * 3 + 2] = 1.0f;

                confidences[i] = Math.max(0.1f, Math.min(1.0f, point.getConfidence()));
            }

            // Calculate proper normals if we have triangles
            if (scanData.getGeometry() != null && scanData.getGeometry().getTriangles() != null) {
                normals = calculateNormals(vertices, scanData.getGeometry().getTriangles());
            }

            // Create vertex buffer
            ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
            bb.order(ByteOrder.nativeOrder());
            vertexBuffer = bb.asFloatBuffer();
            vertexBuffer.put(vertices);
            vertexBuffer.position(0);

            // Create normal buffer
            bb = ByteBuffer.allocateDirect(normals.length * 4);
            bb.order(ByteOrder.nativeOrder());
            normalBuffer = bb.asFloatBuffer();
            normalBuffer.put(normals);
            normalBuffer.position(0);

            // Create confidence buffer
            bb = ByteBuffer.allocateDirect(confidences.length * 4);
            bb.order(ByteOrder.nativeOrder());
            confidenceBuffer = bb.asFloatBuffer();
            confidenceBuffer.put(confidences);
            confidenceBuffer.position(0);

            // Prepare triangle indices
            if (scanData.getGeometry() != null && scanData.getGeometry().getTriangles() != null) {
                List<FaceScanData.FaceGeometry.Triangle> triangles = scanData.getGeometry().getTriangles();
                triangleCount = triangles.size();

                Log.d(TAG, "Preparing " + triangleCount + " triangles");

                short[] indices = new short[triangleCount * 3];
                int validTriangles = 0;

                for (int i = 0; i < triangleCount; i++) {
                    int[] vertexIndices = triangles.get(i).getVertexIndices();

                    // Validate indices
                    if (vertexIndices[0] < vertexCount && vertexIndices[1] < vertexCount && vertexIndices[2] < vertexCount &&
                            vertexIndices[0] >= 0 && vertexIndices[1] >= 0 && vertexIndices[2] >= 0) {

                        indices[validTriangles * 3] = (short) vertexIndices[0];
                        indices[validTriangles * 3 + 1] = (short) vertexIndices[1];
                        indices[validTriangles * 3 + 2] = (short) vertexIndices[2];
                        validTriangles++;
                    }
                }

                triangleCount = validTriangles;
                Log.d(TAG, "Valid triangles: " + triangleCount);

                if (triangleCount > 0) {
                    bb = ByteBuffer.allocateDirect(triangleCount * 3 * 2);
                    bb.order(ByteOrder.nativeOrder());
                    indexBuffer = bb.asShortBuffer();
                    indexBuffer.put(indices, 0, triangleCount * 3);
                    indexBuffer.position(0);
                }
            }

            Log.d(TAG, "Mesh data prepared successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error preparing mesh data", e);
        }
    }

    private float[] calculateNormals(float[] vertices, List<FaceScanData.FaceGeometry.Triangle> triangles) {
        float[] normals = new float[vertices.length];

        // Initialize all normals to zero
        for (int i = 0; i < normals.length; i++) {
            normals[i] = 0.0f;
        }

        // Calculate face normals and accumulate vertex normals
        for (FaceScanData.FaceGeometry.Triangle triangle : triangles) {
            int[] indices = triangle.getVertexIndices();

            // Validate indices
            if (indices[0] * 3 + 2 >= vertices.length ||
                    indices[1] * 3 + 2 >= vertices.length ||
                    indices[2] * 3 + 2 >= vertices.length ||
                    indices[0] < 0 || indices[1] < 0 || indices[2] < 0) {
                continue; // Skip invalid triangles
            }

            // Get triangle vertices
            float[] v1 = {vertices[indices[0] * 3], vertices[indices[0] * 3 + 1], vertices[indices[0] * 3 + 2]};
            float[] v2 = {vertices[indices[1] * 3], vertices[indices[1] * 3 + 1], vertices[indices[1] * 3 + 2]};
            float[] v3 = {vertices[indices[2] * 3], vertices[indices[2] * 3 + 1], vertices[indices[2] * 3 + 2]};

            // Calculate edge vectors
            float[] edge1 = {v2[0] - v1[0], v2[1] - v1[1], v2[2] - v1[2]};
            float[] edge2 = {v3[0] - v1[0], v3[1] - v1[1], v3[2] - v1[2]};

            // Calculate face normal (cross product)
            float[] faceNormal = {
                    edge1[1] * edge2[2] - edge1[2] * edge2[1],
                    edge1[2] * edge2[0] - edge1[0] * edge2[2],
                    edge1[0] * edge2[1] - edge1[1] * edge2[0]
            };

            // Normalize face normal
            float length = (float) Math.sqrt(faceNormal[0] * faceNormal[0] +
                    faceNormal[1] * faceNormal[1] +
                    faceNormal[2] * faceNormal[2]);
            if (length > 0.0001f) {
                faceNormal[0] /= length;
                faceNormal[1] /= length;
                faceNormal[2] /= length;

                // Add face normal to vertex normals
                for (int index : indices) {
                    normals[index * 3] += faceNormal[0];
                    normals[index * 3 + 1] += faceNormal[1];
                    normals[index * 3 + 2] += faceNormal[2];
                }
            }
        }

        // Normalize vertex normals
        for (int i = 0; i < normals.length; i += 3) {
            float length = (float) Math.sqrt(normals[i] * normals[i] +
                    normals[i + 1] * normals[i + 1] +
                    normals[i + 2] * normals[i + 2]);
            if (length > 0.0001f) {
                normals[i] /= length;
                normals[i + 1] /= length;
                normals[i + 2] /= length;
            } else {
                // Default normal pointing forward
                normals[i] = 0.0f;
                normals[i + 1] = 0.0f;
                normals[i + 2] = 1.0f;
            }
        }

        return normals;
    }

    public void draw(float[] mvpMatrix) {
        if (!initialized || vertexBuffer == null) {
            return;
        }

        try {
            // Calculate animation time
            float time = (System.currentTimeMillis() - startTime) / 1000.0f;

            // Clear any previous OpenGL errors
            int error = GLES20.glGetError();
            if (error != GLES20.GL_NO_ERROR) {
                Log.w(TAG, "OpenGL error before draw: " + error);
            }

            // Draw mesh (filled triangles)
            if ((renderMode == RenderMode.MESH_ONLY || renderMode == RenderMode.MESH_AND_WIREFRAME || renderMode == RenderMode.ALL)
                    && indexBuffer != null && triangleCount > 0) {
                drawMesh(mvpMatrix);
            }

            // Draw wireframe
            if ((renderMode == RenderMode.WIREFRAME_ONLY || renderMode == RenderMode.POINTS_AND_WIREFRAME ||
                    renderMode == RenderMode.MESH_AND_WIREFRAME || renderMode == RenderMode.ALL)
                    && indexBuffer != null && triangleCount > 0) {
                drawWireframe(mvpMatrix, time);
            }

            // Draw points
            if (renderMode == RenderMode.POINTS_ONLY || renderMode == RenderMode.POINTS_AND_WIREFRAME || renderMode == RenderMode.ALL) {
                drawPoints(mvpMatrix, time);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in draw", e);
        }
    }

    private void drawMesh(float[] mvpMatrix) {
        GLES20.glUseProgram(meshProgram);

        // Set vertex attributes
        GLES20.glEnableVertexAttribArray(meshPositionHandle);
        GLES20.glVertexAttribPointer(meshPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(meshNormalHandle);
        GLES20.glVertexAttribPointer(meshNormalHandle, 3, GLES20.GL_FLOAT, false, 0, normalBuffer);

        GLES20.glEnableVertexAttribArray(meshConfidenceHandle);
        GLES20.glVertexAttribPointer(meshConfidenceHandle, 1, GLES20.GL_FLOAT, false, 0, confidenceBuffer);

        // Set MVP matrix
        GLES20.glUniformMatrix4fv(meshMvpMatrixHandle, 1, false, mvpMatrix, 0);

        // Draw triangles
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, triangleCount * 3, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(meshPositionHandle);
        GLES20.glDisableVertexAttribArray(meshNormalHandle);
        GLES20.glDisableVertexAttribArray(meshConfidenceHandle);
    }

    private void drawWireframe(float[] mvpMatrix, float time) {
        GLES20.glUseProgram(wireframeProgram);

        GLES20.glEnableVertexAttribArray(wireframePositionHandle);
        GLES20.glVertexAttribPointer(wireframePositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glUniformMatrix4fv(wireframeMvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniform1f(wireframeTimeHandle, time);

        // Set line width
        GLES20.glLineWidth(1.5f);

        // Draw wireframe triangles
        for (int i = 0; i < triangleCount * 3; i += 3) {
            indexBuffer.position(i);
            GLES20.glDrawElements(GLES20.GL_LINE_LOOP, 3, GLES20.GL_UNSIGNED_SHORT, indexBuffer);
        }

        GLES20.glDisableVertexAttribArray(wireframePositionHandle);
    }

    private void drawPoints(float[] mvpMatrix, float time) {
        GLES20.glUseProgram(pointProgram);

        GLES20.glEnableVertexAttribArray(pointPositionHandle);
        GLES20.glVertexAttribPointer(pointPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(pointConfidenceHandle);
        GLES20.glVertexAttribPointer(pointConfidenceHandle, 1, GLES20.GL_FLOAT, false, 0, confidenceBuffer);

        GLES20.glUniformMatrix4fv(pointMvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniform1f(pointTimeHandle, time);

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, vertexCount);

        GLES20.glDisableVertexAttribArray(pointPositionHandle);
        GLES20.glDisableVertexAttribArray(pointConfidenceHandle);
    }

    private int createShaderProgram(String vertexShaderCode, String fragmentShaderCode) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        if (vertexShader == 0) {
            return 0;
        }

        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        if (fragmentShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        if (program == 0) {
            Log.e(TAG, "Error creating program");
            return 0;
        }

        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        // Check for linking errors
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == GLES20.GL_FALSE) {
            String error = GLES20.glGetProgramInfoLog(program);
            Log.e(TAG, "Error linking program: " + error);
            GLES20.glDeleteProgram(program);
            return 0;
        }

        // Clean up shaders as they're linked into program now
        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);

        return program;
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        if (shader == 0) {
            Log.e(TAG, "Error creating shader");
            return 0;
        }

        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        // Check for compilation errors
        int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == GLES20.GL_FALSE) {
            String error = GLES20.glGetShaderInfoLog(shader);
            Log.e(TAG, "Error compiling shader: " + error);
            GLES20.glDeleteShader(shader);
            return 0;
        }

        return shader;
    }

    public void cleanup() {
        if (meshProgram != 0) {
            GLES20.glDeleteProgram(meshProgram);
            meshProgram = 0;
        }
        if (wireframeProgram != 0) {
            GLES20.glDeleteProgram(wireframeProgram);
            wireframeProgram = 0;
        }
        if (pointProgram != 0) {
            GLES20.glDeleteProgram(pointProgram);
            pointProgram = 0;
        }
        initialized = false;
    }
}