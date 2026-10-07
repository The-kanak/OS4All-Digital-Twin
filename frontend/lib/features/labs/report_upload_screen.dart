import 'package:flutter/material.dart';
import '../../core/theme/app_theme.dart';
import '../../shared/widgets/healthcare_widgets.dart';

class ReportUploadScreen extends StatefulWidget {
  const ReportUploadScreen({super.key});

  @override
  State<ReportUploadScreen> createState() => _ReportUploadScreenState();
}

class _ReportUploadScreenState extends State<ReportUploadScreen> {
  bool _isUploading = false;
  bool _uploadCompleted = false;
  String _selectedFileName = '';

  void _simulateUpload(String format) {
    setState(() {
      _isUploading = true;
      _selectedFileName = 'synthetic_blood_panel.$format';
    });

    Future.delayed(const Duration(milliseconds: 1400), () {
      if (mounted) {
        setState(() {
          _isUploading = false;
          _uploadCompleted = true;
        });
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(title: const Text('Secure Report Upload (OCR)')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Zero-Trust Health Document Ingestion',
              style: TextStyle(color: AppTheme.textPrimary, fontSize: 22, fontWeight: FontWeight.w800, letterSpacing: -0.5),
            ),
            const SizedBox(height: 8),
            const Text(
              'Upload medical PDFs, PNGs, or JPG scans. Files undergo magic byte validation and automated biomarker extraction. Low confidence results require manual confirmation.',
              style: TextStyle(color: AppTheme.textSecondary, fontSize: 14, height: 1.4),
            ),
            const SizedBox(height: 24),
            if (!_uploadCompleted && !_isUploading) ...[
              HealthcareCard(
                borderColor: AppTheme.cyan.withValues(alpha: 0.4),
                child: Column(
                  children: [
                    const Icon(Icons.cloud_upload_outlined, color: AppTheme.cyan, size: 48),
                    const SizedBox(height: 14),
                    const Text('Select Report File', style: TextStyle(color: AppTheme.textPrimary, fontSize: 17, fontWeight: FontWeight.w700)),
                    const SizedBox(height: 6),
                    const Text('Supports PDF, PNG, JPG (Max 10MB)', style: TextStyle(color: AppTheme.textMuted, fontSize: 13)),
                    const SizedBox(height: 20),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        ElevatedButton.icon(
                          onPressed: () => _simulateUpload('pdf'),
                          icon: const Icon(Icons.picture_as_pdf),
                          label: const Text('Upload PDF'),
                        ),
                        const SizedBox(width: 12),
                        OutlinedButton.icon(
                          style: OutlinedButton.styleFrom(
                            side: const BorderSide(color: AppTheme.cyan),
                            foregroundColor: AppTheme.cyan,
                            padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 12),
                          ),
                          onPressed: () => _simulateUpload('png'),
                          icon: const Icon(Icons.image),
                          label: const Text('Upload Image'),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ] else if (_isUploading) ...[
              HealthcareCard(
                child: Padding(
                  padding: const EdgeInsets.all(20),
                  child: Column(
                    children: [
                      const CircularProgressIndicator(color: AppTheme.cyan),
                      const SizedBox(height: 18),
                      Text('Analyzing $_selectedFileName...', style: const TextStyle(color: AppTheme.textPrimary, fontSize: 16, fontWeight: FontWeight.w700)),
                      const SizedBox(height: 6),
                      const Text('Verifying magic bytes, computing SHA-256 hash, and extracting biomarkers...',
                          style: TextStyle(color: AppTheme.textSecondary, fontSize: 13), textAlign: TextAlign.center),
                    ],
                  ),
                ),
              ),
            ] else ...[
              HealthcareCard(
                borderColor: AppTheme.success.withValues(alpha: 0.5),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Row(
                          children: [
                            Icon(Icons.check_circle, color: AppTheme.success, size: 20),
                            SizedBox(width: 8),
                            Text('OCR Parsing Completed', style: TextStyle(color: AppTheme.textPrimary, fontSize: 16, fontWeight: FontWeight.w700)),
                          ],
                        ),
                        HealthcareBadge(label: 'CONFIDENCE: 96%', color: AppTheme.success),
                      ],
                    ),
                    const SizedBox(height: 14),
                    const Text('Extracted Biomarkers for Review:', style: TextStyle(color: AppTheme.cyan, fontSize: 13, fontWeight: FontWeight.w700)),
                    const SizedBox(height: 10),
                    _buildExtractedRow('Fasting Blood Glucose', '92.5 mg/dL', '0.97', true),
                    _buildExtractedRow('ALT / SGPT', '28.0 U/L', '0.96', true),
                    _buildExtractedRow('Serum Creatinine', '0.94 mg/dL', '0.95', true),
                    const SizedBox(height: 18),
                    SizedBox(
                      width: double.infinity,
                      child: ElevatedButton(
                        onPressed: () {
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text('Report confirmed and incorporated into your medical timeline.')),
                          );
                          Navigator.of(context).pop();
                        },
                        child: const Text('Confirm & Add to Health Timeline'),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildExtractedRow(String name, String value, String conf, bool confirmed) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(name, style: const TextStyle(color: AppTheme.textPrimary, fontSize: 14, fontWeight: FontWeight.w600)),
          Row(
            children: [
              Text(value, style: const TextStyle(color: AppTheme.cyan, fontSize: 14, fontWeight: FontWeight.w700)),
              const SizedBox(width: 8),
              const Icon(Icons.check, color: AppTheme.success, size: 16),
            ],
          ),
        ],
      ),
    );
  }
}
