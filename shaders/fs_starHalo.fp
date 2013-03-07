#version 140

uniform vec4 Color;

in vec3 vertex_normal;

out vec4 fragColor;

void main() {
	vec3 matColor = vec3(Color.r, Color.g, Color.b);	
	vec3 eye_direction = vec3(0.0, 0.0, 1.0);
	
	float dotP = dot(vertex_normal, eye_direction);	

	//fragColor = vec4(matColor,0.5-(dotP/2.0));
	fragColor = vec4(matColor, dotP*dotP*dotP);
} 