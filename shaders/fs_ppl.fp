#version 140

uniform vec4 HaloColor;

out vec4 fragColor;

void main() {	
	fragColor = HaloColor;
	
	if (HaloColor.a > 1.0) {
		fragColor.rgb = HaloColor.rgb * HaloColor.a;
	}
}
