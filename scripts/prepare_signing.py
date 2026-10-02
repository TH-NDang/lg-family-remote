#!/usr/bin/env python3
"""Decode ONE GitHub secret; never print a key, password, or untrusted exception."""
import base64
import json
import os
from pathlib import Path


def main() -> None:
    raw = os.environ.get("ANDROID_SIGNING_BUNDLE", "").strip()
    with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as out:
        if not raw:
            out.write("ready=false\n")
            print("No signing secret: building preview APK only.")
            return
        try:
            bundle = json.loads(raw)
            fields = {k: bundle[k] for k in ("store_password", "key_alias", "key_password")}
            for value in fields.values():
                if not isinstance(value, str) or not value or len(value) > 256 or '\n' in value or '\r' in value:
                    raise ValueError("Invalid signing field")
            key = base64.b64decode(bundle["keystore_base64"], validate=True)
            if not 100 <= len(key) <= 40000:
                raise ValueError("Invalid keystore length")
        except Exception:
            raise SystemExit("Invalid ANDROID_SIGNING_BUNDLE. See docs/SIGNING.md; secret contents are not logged.")
        directory = Path(os.environ["RUNNER_TEMP"]) / "family-signing"
        directory.mkdir(mode=0o700, exist_ok=True)
        path = directory / "family.jks"
        path.write_bytes(key)
        path.chmod(0o600)
        values = {
            "ANDROID_KEYSTORE_PATH": str(path),
            "ANDROID_STORE_PASSWORD": fields["store_password"],
            "ANDROID_KEY_ALIAS": fields["key_alias"],
            "ANDROID_KEY_PASSWORD": fields["key_password"],
        }
        with open(os.environ["GITHUB_ENV"], "a", encoding="utf-8") as env:
            for name, value in values.items():
                print("::add-mask::" + value.replace("%", "%25"))
                env.write(f"{name}={value}\n")
        out.write("ready=true\n")
        print("Release signing material prepared in runner temporary storage.")

if __name__ == "__main__":
    main()
