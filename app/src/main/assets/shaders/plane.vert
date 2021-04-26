uniform mat4 u_Model;
uniform mat4 u_ModelViewProjection;
uniform mat2 u_PlaneUvMatrix;
uniform vec3 u_Normal;

attribute vec3 a_XZPositionAlpha; // (x, z, alpha)

varying vec3 v_TexCoordAlpha;

void main() {
   vec4 local_pos = vec4(a_XZPositionAlpha.x, 0.0, a_XZPositionAlpha.y, 1.0);
   vec4 world_pos = u_Model * local_pos;

   // Construct two vectors that are orthogonal to the normal.
   // This arbitrary choice is not co-linear with either horizontal
   // or vertical plane normals.
   const vec3 arbitrary = vec3(1.0, 1.0, 0.0);
   vec3 vec_u = normalize(cross(u_Normal, arbitrary));
   vec3 vec_v = normalize(cross(u_Normal, vec_u));

   // Project vertices in world frame onto vec_u and vec_v.
   vec2 uv = vec2(dot(world_pos.xyz, vec_u), dot(world_pos.xyz, vec_v));
   v_TexCoordAlpha = vec3(u_PlaneUvMatrix * uv, a_XZPositionAlpha.z);
   gl_Position = u_ModelViewProjection * local_pos;
}