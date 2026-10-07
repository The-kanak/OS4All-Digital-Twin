import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../core/theme/mobile_design_system.dart';
import '../../shared/widgets/os4_components.dart';
import '../digitaltwin/digital_twin_controller.dart';
import '../healthconnect/health_connect_model.dart';
import 'edit_profile_screen.dart';
import '../timeline/clinical_timeline_screen.dart';
import '../dashboard/demo_mode_screen.dart';
import '../specialty/advanced_tools_screen.dart';
import '../settings/settings_screen.dart';

/// SCREEN 5 — PROFILE
/// Patient demographics, physiological baselines, connected data platforms,
/// and secondary navigation pathways.
class ProfileScreen extends StatelessWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final controller = DigitalTwinController();

    return ListenableBuilder(
      listenable: controller,
      builder: (context, _) {
        final profile = controller.userProfile;
        final hcStatus = controller.healthConnectStatus;
        final isCustom = profile['isCustomBaseline'] == true;

        final fullName = profile['fullName']?.toString() ?? 'Shara Senger';
        final age = profile['age']?.toString() ?? '54';
        final gender = profile['gender']?.toString() ?? 'Female';
        final condition = profile['primaryCondition']?.toString() ?? 'Type 2 Diabetes (Synthea FHIR)';
        final email = profile['email']?.toString() ?? 'shara.senger@synthea.health';
        final height = profile['height']?.toString() ?? '168 cm';
        final weight = profile['weight']?.toString() ?? '78 kg';
        final hba1c = profile['targetHba1c']?.toString() ?? (profile['hba1c']?.toString() ?? '8.2%');
        final basalGlucose = profile['basalGlucose']?.toString() ?? '118 mg/dL';
        final insulinSens = profile['insulinSensitivity']?.toString() ?? '45 mg/dL/U';
        final carbRatio = profile['carbRatio']?.toString() ?? '12 g/U';

        return Scaffold(
          backgroundColor: MobileTheme.background,
          body: SafeArea(
            child: SingleChildScrollView(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // 1. Header
                  _buildHeader(),
                  const SizedBox(height: 12),

                  // 2. Patient Identity Card with Avatar
                  _buildPatientIdentityCard(context, fullName, age, gender, condition, email),
                  const SizedBox(height: 14),

                  // 3. Primary Action: Edit Profile Button
                  PrimaryButton(
                    label: 'Edit Profile & Baselines',
                    icon: Icons.edit_note_rounded,
                    onPressed: () {
                      Navigator.of(context).push(
                        MaterialPageRoute(builder: (_) => const EditProfileScreen()),
                      );
                    },
                  ),
                  const SizedBox(height: 18),

                  // 4. Clinical & Physiological Baseline Card
                  _buildBaselineCard(isCustom, height, weight, basalGlucose, hba1c, insulinSens, carbRatio),
                  const SizedBox(height: 18),

                  // 5. Secondary Navigation Sections
                  _buildSecondarySections(context, hcStatus),
                  const SizedBox(height: 18),

                  // 6. About OS4All Digital Twin
                  _buildAboutCard(),
                  const SizedBox(height: 16),

                  // Bottom Disclaimer Banner
                  const PrototypeDisclaimerBanner(),
                  const SizedBox(height: 20),
                ],
              ),
            ),
          ),
        );
      },
    );
  }

  Widget _buildHeader() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Profile & Settings',
          style: GoogleFonts.inter(
            fontSize: 22,
            fontWeight: FontWeight.w800,
            color: MobileTheme.textPrimary,
            letterSpacing: -0.4,
          ),
        ),
        const SizedBox(height: 2),
        Text(
          'Patient demographics, physiological baselines & system tools.',
          style: GoogleFonts.inter(fontSize: 12, color: MobileTheme.textSecondary),
        ),
      ],
    );
  }

  Widget _buildPatientIdentityCard(
    BuildContext context,
    String name,
    String age,
    String gender,
    String condition,
    String email,
  ) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: MobileTheme.surface,
        borderRadius: MobileTheme.cardRadius,
        border: Border.all(color: MobileTheme.border),
      ),
      child: Column(
        children: [
          Row(
            children: [
              Container(
                width: 56,
                height: 56,
                decoration: BoxDecoration(
                  gradient: const LinearGradient(
                    colors: [Color(0xFF2563EB), Color(0xFF3B82F6)],
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                  ),
                  borderRadius: BorderRadius.circular(16),
                ),
                child: const Center(
                  child: Icon(Icons.person_rounded, color: Colors.white, size: 30),
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Wrap(
                      crossAxisAlignment: WrapCrossAlignment.center,
                      spacing: 6,
                      runSpacing: 2,
                      children: [
                        Text(
                          name.toUpperCase(),
                          style: GoogleFonts.inter(
                            fontSize: 15,
                            fontWeight: FontWeight.w800,
                            color: MobileTheme.textPrimary,
                            letterSpacing: 0.2,
                          ),
                          overflow: TextOverflow.ellipsis,
                        ),
                        const StatusPill(label: 'Virtual Patient', color: MobileTheme.primary),
                      ],
                    ),
                    const SizedBox(height: 3),
                    Text(
                      '$age yo • $gender • MRN: SEN-9821',
                      style: GoogleFonts.inter(fontSize: 12, color: MobileTheme.textSecondary, fontWeight: FontWeight.w500),
                    ),
                    Text(
                      condition,
                      style: GoogleFonts.inter(fontSize: 11.5, color: MobileTheme.primary),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildBaselineCard(
    bool isCustom,
    String height,
    String weight,
    String basalGlucose,
    String hba1c,
    String insulinSens,
    String carbRatio,
  ) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: MobileTheme.surface,
        borderRadius: MobileTheme.cardRadius,
        border: Border.all(color: MobileTheme.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Wrap(
            alignment: WrapAlignment.spaceBetween,
            crossAxisAlignment: WrapCrossAlignment.center,
            spacing: 8,
            runSpacing: 4,
            children: [
              Text(
                'PHYSIOLOGICAL BASELINE',
                style: GoogleFonts.inter(
                  fontSize: 11.5,
                  fontWeight: FontWeight.w800,
                  color: MobileTheme.textPrimary,
                  letterSpacing: 0.5,
                ),
              ),
              StatusPill(
                label: isCustom ? 'Custom Baseline' : 'Synthea Baseline',
                color: isCustom ? MobileTheme.drift : MobileTheme.stable,
              ),
            ],
          ),
          const SizedBox(height: 12),
          _buildRow('Height / Weight', '$height / $weight'),
          _buildRow('Basal Fasting Glucose', basalGlucose),
          _buildRow('Target HbA1c', hba1c),
          _buildRow('Insulin Sensitivity (S_I)', insulinSens),
          _buildRow('Carbohydrate Ratio (C_R)', carbRatio),
        ],
      ),
    );
  }

  Widget _buildSecondarySections(BuildContext context, HealthConnectStatus hcStatus) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'HEALTH PROFILE & NAVIGATION',
          style: GoogleFonts.inter(
            fontSize: 11.5,
            fontWeight: FontWeight.w800,
            color: MobileTheme.textPrimary,
            letterSpacing: 0.5,
          ),
        ),
        const SizedBox(height: 8),
        Container(
          width: double.infinity,
          decoration: BoxDecoration(
            color: MobileTheme.surface,
            borderRadius: MobileTheme.cardRadius,
            border: Border.all(color: MobileTheme.border),
          ),
          child: Column(
            children: [
              _buildNavTile(
                icon: Icons.history_edu_rounded,
                color: MobileTheme.primary,
                title: 'Clinical Timeline',
                subtitle: 'Longitudinal events & disease progression',
                onTap: () {
                  Navigator.of(context).push(
                    MaterialPageRoute(builder: (_) => const ClinicalTimelineScreen()),
                  );
                },
              ),
              const Divider(color: MobileTheme.border, height: 1),
              _buildNavTile(
                icon: Icons.play_lesson_rounded,
                color: MobileTheme.geminiPurple,
                title: 'Live Demo Mode',
                subtitle: 'Guided 6-step competition pitch presentation',
                onTap: () {
                  Navigator.of(context).push(
                    MaterialPageRoute(builder: (_) => const DemoModeScreen()),
                  );
                },
              ),
              const Divider(color: MobileTheme.border, height: 1),
              _buildNavTile(
                icon: Icons.hub_rounded,
                color: MobileTheme.secondaryAccent,
                title: 'Advanced Intelligence & FHIR',
                subtitle: 'Differential equations, RK4 solver & literature',
                onTap: () {
                  Navigator.of(context).push(
                    MaterialPageRoute(builder: (_) => const AdvancedToolsScreen()),
                  );
                },
              ),
              const Divider(color: MobileTheme.border, height: 1),
              _buildNavTile(
                icon: Icons.settings_outlined,
                color: MobileTheme.textSecondary,
                title: 'Application Settings',
                subtitle: 'Backend URL, preferences & telemetry modes',
                onTap: () {
                  Navigator.of(context).push(
                    MaterialPageRoute(builder: (_) => const SettingsScreen()),
                  );
                },
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildNavTile({
    required IconData icon,
    required Color color,
    required String title,
    required String subtitle,
    required VoidCallback onTap,
  }) {
    return Material(
      color: Colors.transparent,
      child: ListTile(
        contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 2),
        leading: Container(
          width: 36,
          height: 36,
          decoration: BoxDecoration(
            color: color.withOpacity(0.12),
            borderRadius: BorderRadius.circular(10),
          ),
          child: Icon(icon, color: color, size: 20),
        ),
        title: Text(
          title,
          style: GoogleFonts.inter(
            fontSize: 13.5,
            fontWeight: FontWeight.w700,
            color: MobileTheme.textPrimary,
          ),
        ),
        subtitle: Text(
          subtitle,
          style: GoogleFonts.inter(fontSize: 11, color: MobileTheme.textSecondary),
        ),
        trailing: const Icon(Icons.chevron_right_rounded, color: MobileTheme.textSecondary, size: 20),
        onTap: onTap,
      ),
    );
  }

  Widget _buildAboutCard() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: MobileTheme.surface,
        borderRadius: MobileTheme.cardRadius,
        border: Border.all(color: MobileTheme.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 8,
                height: 8,
                decoration: const BoxDecoration(
                  color: MobileTheme.primary,
                  shape: BoxShape.circle,
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  'OS4All Digital Twin Platform',
                  style: GoogleFonts.inter(
                    fontSize: 13,
                    fontWeight: FontWeight.w800,
                    color: MobileTheme.textPrimary,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Text(
            'Personal Digital Twin for early metabolic anomaly detection. Fuses static Synthea FHIR health records with dynamic continuous telemetry to project 2-hour trajectory shifts via 4th-order Runge-Kutta numerical modeling.',
            style: GoogleFonts.inter(
              fontSize: 12,
              color: MobileTheme.textSecondary,
              height: 1.45,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            'Version 2.4-mobile-twin • Target 392dp Android',
            style: GoogleFonts.inter(fontSize: 10.5, color: MobileTheme.textSubtle),
          ),
        ],
      ),
    );
  }

  Widget _buildRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Expanded(
            child: Text(
              label,
              style: GoogleFonts.inter(fontSize: 12, color: MobileTheme.textSecondary),
            ),
          ),
          const SizedBox(width: 8),
          Text(
            value,
            style: GoogleFonts.inter(fontSize: 12, fontWeight: FontWeight.w600, color: MobileTheme.textPrimary),
          ),
        ],
      ),
    );
  }
}
