#!/bin/sh
openssl aes-256-cbc -K $encrypted_c48080ac1120_key -iv $encrypted_c48080ac1120_iv -in travis-deploy-key.enc -out travis-deploy-key -d
chmod 600 travis-deploy-key;
cp travis-deploy-key ~/.ssh/id_rsa;
