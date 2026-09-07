# APK-only image. Built in GitHub Actions after assembleRelease.
# Extract: docker create --name wx IMAGE && docker cp wx:/apk/wetter.apk ./wetter.apk && docker rm wx
FROM alpine:3.21
COPY wetter.apk /apk/wetter.apk
LABEL org.opencontainers.image.title="Wetter Android APK" \
      org.opencontainers.image.description="Sideload APK for Wetter Schweiz" \
      org.opencontainers.image.source="https://github.com/rolfwalker71-commits/androidweather"
CMD ["cat", "/apk/wetter.apk"]
