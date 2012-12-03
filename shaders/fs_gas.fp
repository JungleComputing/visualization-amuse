#version 140

uniform float gas_opacity_factor;
uniform vec4 node_color;

out vec4 fragColor;

void main() {
	fragColor = node_color * gas_opacity_factor;
}
