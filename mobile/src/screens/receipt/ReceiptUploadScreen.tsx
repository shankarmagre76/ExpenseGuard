import React, { useState } from 'react';
import {
  View,
  StyleSheet,
  Image,
  TouchableOpacity,
  ScrollView,
  Alert,
} from 'react-native';
import { launchCamera, launchImageLibrary, ImagePickerResponse } from 'react-native-image-picker';
import { ScreenContainer } from '../../components/ScreenContainer';
import { AppText } from '../../components/AppText';
import { PrimaryButton } from '../../components/PrimaryButton';
import { LoadingIndicator } from '../../components/LoadingIndicator';
import { ErrorMessage } from '../../components/ErrorMessage';
import { useReceipts } from '../../hooks/useReceipts';
import { SelectedImage } from '../../types/receipt';
import { colors, spacing, borderRadius } from '../../theme';

const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB limit per backend constraint

export const ReceiptUploadScreen: React.FC<any> = ({ navigation }) => {
  const { uploading, error, setError, uploadAndScan } = useReceipts();
  const [selectedImage, setSelectedImage] = useState<SelectedImage | null>(null);

  const handlePickerResponse = (response: ImagePickerResponse) => {
    if (response.didCancel) {
      return;
    }
    if (response.errorCode) {
      setError(`Camera/Gallery error: ${response.errorMessage || response.errorCode}`);
      return;
    }
    if (response.assets && response.assets.length > 0) {
      const asset = response.assets[0];
      if (!asset.uri) {
        setError('Selected file has invalid URI');
        return;
      }

      if (asset.fileSize && asset.fileSize > MAX_FILE_SIZE_BYTES) {
        setError('Selected file exceeds maximum limit of 10MB.');
        return;
      }

      setError(null);
      setSelectedImage({
        uri: asset.uri,
        fileName: asset.fileName || 'receipt.jpg',
        type: asset.type || 'image/jpeg',
        fileSize: asset.fileSize,
      });
    }
  };

  const handleTakePhoto = () => {
    launchCamera(
      {
        mediaType: 'photo',
        quality: 0.8,
        saveToPhotos: false,
      },
      handlePickerResponse
    );
  };

  const handleSelectFromLibrary = () => {
    launchImageLibrary(
      {
        mediaType: 'photo',
        quality: 0.8,
      },
      handlePickerResponse
    );
  };

  const handleUploadAndScan = async () => {
    if (!selectedImage) {
      setError('Please select or capture a receipt image first.');
      return;
    }

    try {
      const receipt = await uploadAndScan(selectedImage);
      // Navigate to OCR review screen with backend OCR response
      navigation.navigate('OcrReview', { receipt });
    } catch (err: any) {
      Alert.alert('Upload Failed', err.message || 'Unable to process receipt OCR');
    }
  };

  return (
    <ScreenContainer scrollable>
      <ScrollView contentContainerStyle={styles.container}>
        <AppText variant="header" style={styles.title}>
          Scan Receipt
        </AppText>
        <AppText variant="body" color={colors.textSecondary} style={styles.subtitle}>
          Capture or select a receipt photo to automatically extract merchant, date, and amount.
        </AppText>

        {error && <ErrorMessage message={error} style={styles.errorBanner} />}

        {/* Selection buttons */}
        <View style={styles.buttonRow}>
          <TouchableOpacity
            style={styles.pickerOption}
            onPress={handleTakePhoto}
            accessibilityLabel="Take Photo with Camera"
            accessibilityRole="button"
          >
            <AppText variant="subheader" color={colors.primary} style={styles.optionText}>
              📷 Take Photo
            </AppText>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.pickerOption}
            onPress={handleSelectFromLibrary}
            accessibilityLabel="Choose from Gallery"
            accessibilityRole="button"
          >
            <AppText variant="subheader" color={colors.primary} style={styles.optionText}>
              🖼 Choose Photo
            </AppText>
          </TouchableOpacity>
        </View>

        {/* Image Preview */}
        {selectedImage ? (
          <View style={styles.previewCard}>
            <View style={styles.previewHeader}>
              <AppText variant="body" bold numberOfLines={1} style={styles.fileName}>
                {selectedImage.fileName}
              </AppText>
              <TouchableOpacity onPress={() => setSelectedImage(null)}>
                <AppText variant="caption" color={colors.error} bold>
                  Clear
                </AppText>
              </TouchableOpacity>
            </View>
            <Image source={{ uri: selectedImage.uri }} style={styles.previewImage} resizeMode="contain" />
          </View>
        ) : (
          <View style={styles.placeholderContainer}>
            <AppText variant="caption" color={colors.textMuted} align="center">
              No image selected. Tap above to take a photo or select an image from your device.
            </AppText>
          </View>
        )}

        {/* Action Button */}
        <View style={styles.actionContainer}>
          <PrimaryButton
            title="Scan & Upload Receipt"
            onPress={handleUploadAndScan}
            isLoading={uploading}
            disabled={!selectedImage || uploading}
          />
        </View>

        {uploading && <LoadingIndicator message="Uploading image & extracting OCR text..." />}
      </ScrollView>
    </ScreenContainer>
  );
};

const styles = StyleSheet.create({
  container: {
    paddingBottom: spacing.xl,
  },
  title: {
    marginBottom: spacing.xs,
  },
  subtitle: {
    marginBottom: spacing.lg,
  },
  errorBanner: {
    marginBottom: spacing.md,
  },
  buttonRow: {
    flexDirection: 'row',
    gap: spacing.md,
    marginBottom: spacing.lg,
  },
  pickerOption: {
    flex: 1,
    backgroundColor: colors.surface,
    padding: spacing.md,
    borderRadius: borderRadius.md,
    borderWidth: 1.5,
    borderColor: colors.primary,
    alignItems: 'center',
  },
  optionText: {
    fontWeight: '700',
  },
  previewCard: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.md,
    borderWidth: 1,
    borderColor: colors.border,
    marginBottom: spacing.lg,
  },
  previewHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing.sm,
  },
  fileName: {
    flex: 1,
    marginRight: spacing.sm,
  },
  previewImage: {
    width: '100%',
    height: 240,
    borderRadius: borderRadius.md,
    backgroundColor: colors.background,
  },
  placeholderContainer: {
    backgroundColor: colors.surface,
    borderRadius: borderRadius.lg,
    padding: spacing.xxl,
    borderWidth: 1,
    borderColor: colors.border,
    borderStyle: 'dashed',
    marginBottom: spacing.lg,
    alignItems: 'center',
  },
  actionContainer: {
    marginTop: spacing.sm,
  },
});
