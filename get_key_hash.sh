#!/bin/bash
keytool -list -v -keystore release.keystore -alias duwave -storepass 'Du"@"_wave/341' | grep "SHA256"
