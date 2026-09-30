"""Regenerates all Java registries, assets, data, models and textures for The Sift."""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import gen_assets  # noqa: E402
import gen_data  # noqa: E402
import gen_java  # noqa: E402
import gen_models  # noqa: E402
import gen_textures  # noqa: E402
import gen_world  # noqa: E402

if __name__ == '__main__':
    gen_java.main()
    gen_assets.generate()
    gen_world.generate()
    gen_data.generate()
    gen_assets.finalize()
    gen_models.main('--no-preview' not in sys.argv)
    gen_textures.main()
