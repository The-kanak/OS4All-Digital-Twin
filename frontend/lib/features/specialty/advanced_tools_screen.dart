import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../core/network/app_config.dart';
import '../../core/theme/mobile_design_system.dart';

/// ADVANCED TOOLS & CLINICAL EVIDENCE
/// Houses secondary specialist tools without cluttering the primary mobile experience:
/// 1. Synthea FHIR Resource Inspector
/// 2. Deterministic Intelligence Trace
/// 3. Clinical Literature & Evidence Citations
/// 4. Backend System Connectivity
class AdvancedToolsScreen extends StatelessWidget {
  const AdvancedToolsScreen({super.key});

  @override
  Widget build(BuildContext context) {
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
          'Advanced Tools & Intelligence',
          style: GoogleFonts.inter(
            color: MobileTheme.textPrimary,
            fontSize: 17,
            fontWeight: FontWeight.w700,
          ),
        ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const PrototypeDisclaimerBanner(),
              const SizedBox(height: 14),

              // 1. Synthea FHIR Diagnostic Card
              _buildSectionTitle('SYNTHEA FHIR RESOURCE INSPECTOR'),
              const SizedBox(height: 8),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: MobileTheme.surface,
                  borderRadius: MobileTheme.cardRadius,
                  border: Border.all(color: MobileTheme.border),
                ),
                child: Column(
                  children: [
                    _buildRow('FHIR Patient ID', 'urn:uuid:shara-senger-uuid'),
                    _buildRow('SNOMED Condition Code', '44054006 (Type 2 Diabetes)'),
                    _buildRow('Diagnostic Observation', 'HbA1c: 8.2% (LOINC 4548-4)'),
                    _buildRow('Fasting Blood Glucose', '158 mg/dL (LOINC 1558-6)'),
                    _buildRow('Static Record Status', 'Ingested & Normalized'),
                  ],
                ),
              ),
              const SizedBox(height: 18),

              // 2. Deterministic Intelligence Trace
              _buildSectionTitle('DETERMINISTIC MATH & RK4 TRACE'),
              const SizedBox(height: 8),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: MobileTheme.surface,
                  borderRadius: MobileTheme.cardRadius,
                  border: Border.all(color: MobileTheme.border),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Differential Equations for 2-Hour Trajectory:',
                      style: GoogleFonts.inter(
                        fontSize: 12.5,
                        fontWeight: FontWeight.w700,
                        color: MobileTheme.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 6),
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        color: MobileTheme.surfaceElevated,
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(color: MobileTheme.border),
                      ),
                      child: Text(
                        'dG/dt = EGP - k_decay * G(t) - S_I * I_eff(t) * G(t) + dG_meal/dt\n'
                        'Composite Risk = 0.4 * G_norm + 0.3 * V_velocity + 0.3 * H_hrv',
                        style: GoogleFonts.robotoMono(
                          fontSize: 11,
                          color: MobileTheme.primary,
                        ),
                      ),
                    ),
                    const SizedBox(height: 10),
                    _buildRow('Solver Algorithm', '4th-Order Runge-Kutta (RK4)'),
                    _buildRow('Projection Interval', '120 Minutes (24 x 5-min steps)'),
                    _buildRow('State Machine Bound', 'Euler Convergence Checked'),
                  ],
                ),
              ),
              const SizedBox(height: 18),

              // 3. Clinical Literature & Evidence Citations
              _buildSectionTitle('CLINICAL EVIDENCE CITATIONS'),
              const SizedBox(height: 8),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: MobileTheme.surface,
                  borderRadius: MobileTheme.cardRadius,
                  border: Border.all(color: MobileTheme.border),
                ),
                child: Column(
                  children: [
                    _buildEvidenceTile(
                      title: 'DCCT / EDIC Research Group (1993, 2016)',
                      citation: 'Longitudinal glycemic variance correlates with microvascular risk.',
                    ),
                    const Divider(color: MobileTheme.border, height: 16),
                    _buildEvidenceTile(
                      title: 'UKPDS 33 Trial (Lancet 1998)',
                      citation: 'Intensive blood-glucose control reduces diabetic complication progression.',
                    ),
                    const Divider(color: MobileTheme.border, height: 16),
                    _buildEvidenceTile(
                      title: 'American Diabetes Association (Standards of Care 2024)',
                      citation: 'CGM metrics: Time in Range (70-180 mg/dL) target > 70% of 24h cycle.',
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 18),

              // 4. Backend System Connectivity
              _buildSectionTitle('BACKEND SYSTEM ARCHITECTURE'),
              const SizedBox(height: 8),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: MobileTheme.surface,
                  borderRadius: MobileTheme.cardRadius,
                  border: Border.all(color: MobileTheme.border),
                ),
                child: Column(
                  children: [
                    _buildRow('Backend Server', 'Spring Boot 3.3.4 (Port 8082)'),
                    _buildRow('Active Base URL', AppConfig.apiBaseUrl),
                    _buildRow('Gemini Model', 'gemini-3.8-flash (Google GenAI)'),
                    _buildRow('Health Connect Bridge', 'MethodChannel "os4all/health_connect"'),
                    _buildRow('Client Build', 'Flutter 3.x Mobile-First Target (392dp)'),
                  ],
                ),
              ),
              const SizedBox(height: 20),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildSectionTitle(String title) {
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

  Widget _buildRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Flexible(
            child: Text(
              label,
              style: GoogleFonts.inter(fontSize: 11.5, color: MobileTheme.textSecondary),
              overflow: TextOverflow.ellipsis,
            ),
          ),
          const SizedBox(width: 8),
          Flexible(
            child: Text(
              value,
              style: GoogleFonts.inter(fontSize: 11.5, fontWeight: FontWeight.w600, color: MobileTheme.textPrimary),
              overflow: TextOverflow.ellipsis,
              textAlign: TextAlign.end,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildEvidenceTile({required String title, required String citation}) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style: GoogleFonts.inter(fontSize: 12.5, fontWeight: FontWeight.w700, color: MobileTheme.primary),
        ),
        const SizedBox(height: 2),
        Text(
          citation,
          style: GoogleFonts.inter(fontSize: 11.5, color: MobileTheme.textSecondary),
        ),
      ],
    );
  }
}
