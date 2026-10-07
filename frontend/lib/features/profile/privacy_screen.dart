import 'package:flutter/material.dart';
import '../../core/theme/app_theme.dart';
import '../../shared/widgets/healthcare_widgets.dart';

class PrivacyScreen extends StatelessWidget {
  const PrivacyScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(title: const Text('Privacy & Security Architecture')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Zero-Trust Health Data Protection',
              style: TextStyle(color: AppTheme.textPrimary, fontSize: 22, fontWeight: FontWeight.w800, letterSpacing: -0.5),
            ),
            const SizedBox(height: 8),
            const Text(
              'How OS4All guarantees complete privacy, local encryption, and auditability.',
              style: TextStyle(color: AppTheme.textSecondary, fontSize: 14),
            ),
            const SizedBox(height: 20),
            _buildPrincipleCard(
              title: 'Tamper-Evident SHA-256 Audit Trails',
              description: 'Every observation change, consent toggle, and AI inference is immutably logged with SHA-256 cryptographic hash-chaining.',
              icon: Icons.security,
            ),
            const SizedBox(height: 14),
            _buildPrincipleCard(
              title: 'No Direct LLM Database Access',
              description: 'AI models never query patient databases. The AI receives curated, strictly read-only, anonymized health contexts and cannot alter health records.',
              icon: Icons.shield,
            ),
            const SizedBox(height: 14),
            _buildPrincipleCard(
              title: 'Isolated Evidence Separation',
              description: 'Tavily searches and biomedical research citations remain separate from your health timeline, eliminating data leakage.',
              icon: Icons.menu_book,
            ),
            const SizedBox(height: 14),
            _buildPrincipleCard(
              title: 'Zero-Knowledge Client Storage',
              description: 'Client apps never hold master encryption keys or AI service secrets. All sensitive credentials remain securely encapsulated on the backend.',
              icon: Icons.vpn_key_outlined,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPrincipleCard({required String title, required String description, required IconData icon}) {
    return HealthcareCard(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(color: AppTheme.surfaceLight, borderRadius: BorderRadius.circular(10)),
            child: Icon(icon, color: AppTheme.cyan, size: 22),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: const TextStyle(color: AppTheme.textPrimary, fontSize: 15, fontWeight: FontWeight.w700)),
                const SizedBox(height: 4),
                Text(description, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 13, height: 1.4)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
