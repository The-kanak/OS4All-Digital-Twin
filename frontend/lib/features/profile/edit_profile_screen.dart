import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../core/theme/mobile_design_system.dart';
import '../../shared/widgets/os4_components.dart';
import '../digitaltwin/digital_twin_controller.dart';

/// EDIT PROFILE SCREEN
/// Allows modification of patient physiological baselines and demographics.
/// Persists locally via SharedPreferences and updates the active Digital Twin controller.
class EditProfileScreen extends StatefulWidget {
  const EditProfileScreen({super.key});

  @override
  State<EditProfileScreen> createState() => _EditProfileScreenState();
}

class _EditProfileScreenState extends State<EditProfileScreen> {
  final _formKey = GlobalKey<FormState>();

  late TextEditingController _nameController;
  late TextEditingController _ageController;
  late TextEditingController _genderController;
  late TextEditingController _heightController;
  late TextEditingController _weightController;
  late TextEditingController _conditionController;
  late TextEditingController _emailController;
  late TextEditingController _basalGlucoseController;
  late TextEditingController _targetHba1cController;
  late TextEditingController _insulinSensitivityController;
  late TextEditingController _carbRatioController;

  @override
  void initState() {
    super.initState();
    final profile = DigitalTwinController().userProfile;
    _nameController = TextEditingController(text: profile['fullName']?.toString() ?? 'Shara Senger');
    _ageController = TextEditingController(text: (profile['age'] ?? 54).toString());
    _genderController = TextEditingController(text: profile['gender']?.toString() ?? 'Female');
    _heightController = TextEditingController(text: profile['height']?.toString() ?? '168 cm');
    _weightController = TextEditingController(text: profile['weight']?.toString() ?? '78 kg');
    _conditionController = TextEditingController(text: profile['primaryCondition']?.toString() ?? 'Type 2 Diabetes (Synthea FHIR)');
    _emailController = TextEditingController(text: profile['email']?.toString() ?? 'shara.senger@synthea.health');
    _basalGlucoseController = TextEditingController(text: profile['basalGlucose']?.toString() ?? '118 mg/dL');
    _targetHba1cController = TextEditingController(text: profile['targetHba1c']?.toString() ?? '7.0%');
    _insulinSensitivityController = TextEditingController(text: profile['insulinSensitivity']?.toString() ?? '45 mg/dL/U');
    _carbRatioController = TextEditingController(text: profile['carbRatio']?.toString() ?? '12 g/U');
  }

  @override
  void dispose() {
    _nameController.dispose();
    _ageController.dispose();
    _genderController.dispose();
    _heightController.dispose();
    _weightController.dispose();
    _conditionController.dispose();
    _emailController.dispose();
    _basalGlucoseController.dispose();
    _targetHba1cController.dispose();
    _insulinSensitivityController.dispose();
    _carbRatioController.dispose();
    super.dispose();
  }

  void _saveProfile() async {
    if (_formKey.currentState?.validate() ?? false) {
      final parsedAge = int.tryParse(_ageController.text.trim()) ?? 54;
      await DigitalTwinController().updateProfile(
        fullName: _nameController.text.trim(),
        age: parsedAge,
        gender: _genderController.text.trim(),
        height: _heightController.text.trim(),
        weight: _weightController.text.trim(),
        primaryCondition: _conditionController.text.trim(),
        email: _emailController.text.trim(),
        basalGlucose: _basalGlucoseController.text.trim(),
        targetHba1c: _targetHba1cController.text.trim(),
        insulinSensitivity: _insulinSensitivityController.text.trim(),
        carbRatio: _carbRatioController.text.trim(),
      );

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Custom baseline saved locally for active session.', style: TextStyle(color: Colors.white)),
            backgroundColor: MobileTheme.textPrimary,
            duration: Duration(seconds: 2),
          ),
        );
        Navigator.of(context).pop();
      }
    }
  }

  void _restoreDefaults() async {
    await DigitalTwinController().restoreSyntheaDefaults();
    final profile = DigitalTwinController().userProfile;

    setState(() {
      _nameController.text = profile['fullName']?.toString() ?? 'Shara Senger';
      _ageController.text = (profile['age'] ?? 54).toString();
      _genderController.text = profile['gender']?.toString() ?? 'Female';
      _heightController.text = profile['height']?.toString() ?? '168 cm';
      _weightController.text = profile['weight']?.toString() ?? '78 kg';
      _conditionController.text = profile['primaryCondition']?.toString() ?? 'Type 2 Diabetes (Synthea FHIR)';
      _emailController.text = profile['email']?.toString() ?? 'shara.senger@synthea.health';
      _basalGlucoseController.text = profile['basalGlucose']?.toString() ?? '118 mg/dL';
      _targetHba1cController.text = profile['targetHba1c']?.toString() ?? '7.0%';
      _insulinSensitivityController.text = profile['insulinSensitivity']?.toString() ?? '45 mg/dL/U';
      _carbRatioController.text = profile['carbRatio']?.toString() ?? '12 g/U';
    });

    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Restored official Synthea FHIR baseline defaults.'),
          backgroundColor: MobileTheme.surfaceElevated,
          duration: Duration(seconds: 2),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final isCustom = DigitalTwinController().userProfile['isCustomBaseline'] == true;

    return Scaffold(
      backgroundColor: MobileTheme.background,
      appBar: AppBar(
        backgroundColor: MobileTheme.surface,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded, color: MobileTheme.textPrimary),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: Text(
          'Edit Profile & Baseline',
          style: GoogleFonts.inter(
            color: MobileTheme.textPrimary,
            fontSize: 17,
            fontWeight: FontWeight.w700,
          ),
        ),
        actions: [
          TextButton(
            onPressed: _saveProfile,
            child: Text(
              'Save',
              style: GoogleFonts.inter(
                color: MobileTheme.primary,
                fontWeight: FontWeight.w700,
                fontSize: 15,
              ),
            ),
          ),
        ],
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Baseline Mode Indicator Banner
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                  decoration: BoxDecoration(
                    color: isCustom ? MobileTheme.drift.withOpacity(0.12) : MobileTheme.primary.withOpacity(0.12),
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(
                      color: isCustom ? MobileTheme.drift.withOpacity(0.5) : MobileTheme.primary.withOpacity(0.5),
                    ),
                  ),
                  child: Row(
                    children: [
                      Icon(
                        isCustom ? Icons.edit_note_rounded : Icons.verified_user_rounded,
                        color: isCustom ? MobileTheme.drift : MobileTheme.primary,
                        size: 18,
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              isCustom ? 'Custom Baseline (Saved locally)' : 'Synthea FHIR Baseline (Standard)',
                              style: GoogleFonts.inter(
                                fontSize: 12,
                                fontWeight: FontWeight.w700,
                                color: isCustom ? MobileTheme.drift : MobileTheme.primary,
                              ),
                            ),
                            Text(
                              'Updates metabolic coefficients in the active digital twin simulation.',
                              style: GoogleFonts.inter(fontSize: 10.5, color: MobileTheme.textSecondary),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 16),

                // Section 1: Personal Demographics
                _buildSectionHeader('PERSONAL DEMOGRAPHICS'),
                const SizedBox(height: 10),
                _buildTextField('Full Name', _nameController, Icons.person_outline_rounded),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(child: _buildTextField('Age', _ageController, Icons.calendar_today_rounded)),
                    const SizedBox(width: 10),
                    Expanded(child: _buildTextField('Gender', _genderController, Icons.male_rounded)),
                  ],
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(child: _buildTextField('Height', _heightController, Icons.height_rounded)),
                    const SizedBox(width: 10),
                    Expanded(child: _buildTextField('Weight', _weightController, Icons.scale_rounded)),
                  ],
                ),
                const SizedBox(height: 10),
                _buildTextField('Condition', _conditionController, Icons.medical_services_outlined),
                const SizedBox(height: 10),
                _buildTextField('Email', _emailController, Icons.email_outlined),
                const SizedBox(height: 20),

                // Section 2: Metabolic Baseline Parameters
                _buildSectionHeader('METABOLIC BASELINE PARAMETERS'),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(child: _buildTextField('Basal Fasting Glucose', _basalGlucoseController, Icons.water_drop_outlined)),
                    const SizedBox(width: 10),
                    Expanded(child: _buildTextField('Target HbA1c', _targetHba1cController, Icons.percent_rounded)),
                  ],
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(child: _buildTextField('Insulin Sensitivity (S_I)', _insulinSensitivityController, Icons.speed_rounded)),
                    const SizedBox(width: 10),
                    Expanded(child: _buildTextField('Carbohydrate Ratio (C_R)', _carbRatioController, Icons.restaurant_outlined)),
                  ],
                ),
                const SizedBox(height: 24),

                // Action Buttons
                PrimaryButton(
                  label: 'Save Changes',
                  icon: Icons.check_rounded,
                  onPressed: _saveProfile,
                ),
                const SizedBox(height: 10),
                SecondaryButton(
                  label: 'Restore Synthea Defaults',
                  icon: Icons.restore_rounded,
                  onPressed: _restoreDefaults,
                ),
                const SizedBox(height: 20),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildSectionHeader(String title) {
    return Text(
      title,
      style: GoogleFonts.inter(
        fontSize: 11.5,
        fontWeight: FontWeight.w800,
        color: MobileTheme.textPrimary,
        letterSpacing: 0.5,
      ),
    );
  }

  Widget _buildTextField(String label, TextEditingController controller, IconData icon) {
    return Container(
      constraints: const BoxConstraints(minHeight: 48),
      child: TextFormField(
        controller: controller,
        style: GoogleFonts.inter(color: MobileTheme.textPrimary, fontSize: 13.5),
        decoration: InputDecoration(
          labelText: label,
          labelStyle: GoogleFonts.inter(color: MobileTheme.textSecondary, fontSize: 12),
          prefixIcon: Icon(icon, color: MobileTheme.textSecondary, size: 18),
          filled: true,
          fillColor: MobileTheme.surface,
          contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: const BorderSide(color: MobileTheme.border),
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: const BorderSide(color: MobileTheme.border),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: const BorderSide(color: MobileTheme.primary, width: 1.5),
          ),
        ),
        validator: (v) => (v == null || v.trim().isEmpty) ? 'Required' : null,
      ),
    );
  }
}
