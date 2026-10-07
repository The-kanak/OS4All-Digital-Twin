import 'package:flutter/material.dart';
import '../../core/theme/app_theme.dart';
import '../../core/network/app_config.dart';
import '../../shared/widgets/healthcare_widgets.dart';
import '../consent/consent_screen.dart';
import '../profile/privacy_screen.dart';
import '../labs/labs_screen.dart';
import '../auth/login_screen.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  bool _useDemoData = true;
  bool _notificationsEnabled = true;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(title: const Text('Application Settings')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Data & Backend Settings', style: TextStyle(color: AppTheme.cyan, fontSize: 13, fontWeight: FontWeight.w700)),
            const SizedBox(height: 10),
            HealthcareCard(
              child: Column(
                children: [
                  SwitchListTile(
                    contentPadding: EdgeInsets.zero,
                    title: const Text('Demo Mode (Synthetic Data)', style: TextStyle(color: AppTheme.textPrimary, fontSize: 15, fontWeight: FontWeight.w600)),
                    subtitle: const Text('Render realistic synthetic baseline and observation data for development and demonstration.', style: TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
                    value: _useDemoData,
                    activeThumbColor: AppTheme.cyan,
                    onChanged: (val) => setState(() => _useDemoData = val),
                  ),
                  const Divider(color: AppTheme.cardBorder),
                  SwitchListTile(
                    contentPadding: EdgeInsets.zero,
                    title: const Text('Proactive Health Drift Alerts', style: TextStyle(color: AppTheme.textPrimary, fontSize: 15, fontWeight: FontWeight.w600)),
                    subtitle: const Text('Notify when continuous vitals show statistically significant baseline drift (>2 standard deviations).', style: TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
                    value: _notificationsEnabled,
                    activeThumbColor: AppTheme.cyan,
                    onChanged: (val) => setState(() => _notificationsEnabled = val),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),
            const Text('Management & Privacy', style: TextStyle(color: AppTheme.cyan, fontSize: 13, fontWeight: FontWeight.w700)),
            const SizedBox(height: 10),
            HealthcareCard(
              child: Column(
                children: [
                  _buildNavTile(
                    title: 'Laboratory Reports & OCR Ingestion',
                    icon: Icons.science_outlined,
                    onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const LabsScreen())),
                  ),
                  const Divider(color: AppTheme.cardBorder),
                  _buildNavTile(
                    title: 'Consent Preferences',
                    icon: Icons.checklist,
                    onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const ConsentScreen())),
                  ),
                  const Divider(color: AppTheme.cardBorder),
                  _buildNavTile(
                    title: 'Security Architecture & Audit Trails',
                    icon: Icons.shield_outlined,
                    onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const PrivacyScreen())),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),
            HealthcareCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('System Information', style: TextStyle(color: AppTheme.textPrimary, fontSize: 14, fontWeight: FontWeight.w700)),
                  const SizedBox(height: 6),
                  Text('Environment: ${AppConfig.apiBaseUrl}', style: const TextStyle(color: AppTheme.textMuted, fontSize: 12)),
                  const SizedBox(height: 2),
                  const Text('Security: Zero-trust client • Secrets on backend only', style: TextStyle(color: AppTheme.textMuted, fontSize: 12)),
                  const SizedBox(height: 2),
                  const Text('Version: 1.0.0 (Hackathon Build)', style: TextStyle(color: AppTheme.cyanMuted, fontSize: 12)),
                ],
              ),
            ),
            const SizedBox(height: 24),
            SizedBox(
              width: double.infinity,
              child: OutlinedButton.icon(
                style: OutlinedButton.styleFrom(
                  side: const BorderSide(color: AppTheme.danger),
                  foregroundColor: AppTheme.danger,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
                icon: const Icon(Icons.logout),
                label: const Text('Sign Out', style: TextStyle(fontWeight: FontWeight.w700)),
                onPressed: () {
                  Navigator.of(context).pushAndRemoveUntil(
                    MaterialPageRoute(builder: (_) => const LoginScreen()),
                    (route) => false,
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildNavTile({required String title, required IconData icon, required VoidCallback onTap}) {
    return ListTile(
      contentPadding: EdgeInsets.zero,
      leading: Icon(icon, color: AppTheme.cyan, size: 22),
      title: Text(title, style: const TextStyle(color: AppTheme.textPrimary, fontSize: 15, fontWeight: FontWeight.w600)),
      trailing: const Icon(Icons.chevron_right, color: AppTheme.textMuted, size: 20),
      onTap: onTap,
    );
  }
}
