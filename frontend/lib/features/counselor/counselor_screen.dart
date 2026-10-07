import 'package:flutter/material.dart';
import '../../core/theme/app_theme.dart';
import '../../shared/widgets/healthcare_widgets.dart';

class CounselorScreen extends StatefulWidget {
  const CounselorScreen({super.key});

  @override
  State<CounselorScreen> createState() => _CounselorScreenState();
}

class _CounselorMessageData {
  final String role; // 'user' or 'counselor'
  final String message;
  final List<String> observations;
  final List<String> interpretations;
  final List<Map<String, dynamic>> externalEvidence;
  final List<String> actions;
  final String? evalGuidance;
  final String urgency;
  final bool isEmergency;
  final String? disclaimer;

  _CounselorMessageData({
    required this.role,
    required this.message,
    this.observations = const [],
    this.interpretations = const [],
    this.externalEvidence = const [],
    this.actions = const [],
    this.evalGuidance,
    this.urgency = 'ROUTINE',
    this.isEmergency = false,
    this.disclaimer,
  });
}

class _CounselorScreenState extends State<CounselorScreen> {
  final _inputController = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  bool _isLoading = false;

  final List<_CounselorMessageData> _messages = [
    _CounselorMessageData(
      role: 'counselor',
      message: 'Hello! I am your OS4All AI Health Counselor. I have read-only access to your structured baseline metrics, recent trend detections, and confirmed lab results.\n\nI can explain why a status shifted, how it compares to your personal baseline, or clarify evidence retrieved by our system.',
      observations: [
        'Baseline resting HR: 60 bpm (±2.5 bpm)',
        'Baseline HRV: 55 ms (±4.0 ms)',
        'Baseline sleep: 8.0 hrs/night',
      ],
      interpretations: [
        'Historical signals establish steady baseline stability.',
      ],
      actions: [
        'Ask questions regarding your health trends or lab reports anytime.',
      ],
      disclaimer: 'OS4All Counselor provides objective physiological context. It does not diagnose clinical conditions or replace medical professionals.',
    ),
  ];

  void _sendMessage() {
    final text = _inputController.text.trim();
    if (text.isEmpty) return;

    setState(() {
      _messages.add(_CounselorMessageData(
        role: 'user',
        message: text,
      ));
      _isLoading = true;
      _inputController.clear();
    });

    _scrollToBottom();

    // Check emergency trigger or simulation response
    Future.delayed(const Duration(milliseconds: 650), () {
      if (!mounted) return;

      final lower = text.toLowerCase();
      final isEmergency = lower.contains('chest pain') ||
          lower.contains('crushing') ||
          lower.contains('can\'t breathe') ||
          lower.contains('heart attack') ||
          lower.contains('kill myself');

      _CounselorMessageData reply;
      if (isEmergency) {
        reply = _CounselorMessageData(
          role: 'counselor',
          message: 'EMERGENCY ADVISORY: The symptoms you described may represent an acute medical emergency. Please do NOT wait or rely on biometric tracking. Immediately call emergency medical services (911, 112, or local equivalent) or proceed to the nearest emergency room.',
          observations: ['Potential acute clinical emergency detected from message contents.'],
          interpretations: ['Severe physiological presentation requiring immediate hands-on clinical evaluation.'],
          actions: [
            'Call emergency services (911 / 112) immediately.',
            'Alert a family member, neighbor, or companion.',
            'Rest quietly and avoid physical exertion while awaiting emergency medical responders.',
          ],
          evalGuidance: 'Immediate emergency room evaluation is urgently advised.',
          urgency: 'EMERGENCY',
          isEmergency: true,
          disclaimer: 'OS4All safety protocol: Critical acute symptoms require emergency medical care.',
        );
      } else if (lower.contains('why') || lower.contains('status') || lower.contains('change')) {
        reply = _CounselorMessageData(
          role: 'counselor',
          message: 'Based on your actual OS4All physiological baseline, your health status recently shifted to DRIFT due to a multi-signal autonomic deviation. Your resting heart rate elevated above your established baseline while nocturnal HRV decreased across the last 48 hours.',
          observations: [
            'Resting HR measured at 72 bpm (+12 bpm above your 60 bpm personal baseline).',
            'HRV measured at 38 ms (-17 ms below your 55 ms baseline normal range).',
            'Sleep duration averaged 5.2 hrs (vs your 8.0-hr historical average).',
          ],
          interpretations: [
            'Autonomic strain consistent with acute sleep deficit and elevated sympathetic cardiovascular tone.',
            'Potential early physiological adaptation to physical exertion, mental stress, or subclinical recovery debt.',
          ],
          externalEvidence: [
            {
              'title': 'Heart rate variability as a marker of autonomic recovery and physical stress',
              'source': 'Frontiers in Physiology / PubMed',
              'url': 'https://doi.org/10.3389/fphys.2018.00532',
              'relevance': 'Suppressed HRV paired with elevated resting HR reliably correlates with autonomic recovery deficit and sleep disruption.',
            }
          ],
          actions: [
            'Prioritize restorative rest and aim for 8 hours of sleep over the next 2-3 nights.',
            'Moderate high-intensity athletic or cardiovascular training load in favor of active recovery.',
            'Ensure adequate hydration and electrolyte intake.',
          ],
          evalGuidance: 'If resting pulse remains elevated for more than 5 days or acute symptoms occur, consult a medical doctor.',
          urgency: 'MONITOR',
          disclaimer: 'OS4All interpretations are based strictly on personal historical baselines. They do not constitute a medical diagnosis.',
        );
      } else {
        reply = _CounselorMessageData(
          role: 'counselor',
          message: 'Your continuous physiological vitals and biometric signals remain stable and well-aligned with your personal historical baseline. Resting heart rate, HRV, SpO2, and sleep patterns reflect healthy autonomic homeostasis.',
          observations: [
            'Continuous parameters are firmly within your established 95% individual confidence envelope.',
          ],
          interpretations: [
            'Stable autonomic balance and consistent physiological recovery.',
          ],
          actions: [
            'Maintain your regular physical activity, balanced nutrition, and consistent sleep schedule.',
            'Continue wearing your tracker to sustain a statistically robust baseline.',
          ],
          evalGuidance: 'Routine annual check-ups remain standard as no clinical baseline anomalies are currently active.',
          urgency: 'ROUTINE',
          disclaimer: 'OS4All insights are informational health interpretations based strictly on personal historical baselines.',
        );
      }

      setState(() {
        _messages.add(reply);
        _isLoading = false;
      });

      _scrollToBottom();
    });
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        title: const Row(
          children: [
            CircleAvatar(
              radius: 14,
              backgroundColor: AppTheme.surfaceLight,
              child: Icon(Icons.support_agent, color: AppTheme.cyan, size: 18),
            ),
            SizedBox(width: 10),
            Text('OS4All AI Counselor'),
          ],
        ),
      ),
      body: Column(
        children: [
          const Padding(
            padding: EdgeInsets.fromLTRB(16, 10, 16, 0),
            child: DemoDataNoticeBanner(label: 'DEMO DATA — NOT A REAL PATIENT'),
          ),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            color: AppTheme.surfaceLight.withValues(alpha: 0.4),
            child: const Row(
              children: [
                Icon(Icons.shield_outlined, color: AppTheme.cyan, size: 16),
                SizedBox(width: 8),
                Expanded(
                  child: Text(
                    'Conversations are private, auditable, and grounded in your personal baselines.',
                    style: TextStyle(color: AppTheme.textSecondary, fontSize: 11),
                  ),
                ),
              ],
            ),
          ),
          Expanded(
            child: ListView.builder(
              controller: _scrollController,
              padding: const EdgeInsets.all(16),
              itemCount: _messages.length,
              itemBuilder: (context, index) {
                final msg = _messages[index];
                final isUser = msg.role == 'user';
                return _buildMessageBubble(msg, isUser);
              },
            ),
          ),
          if (_isLoading)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 8),
              child: HealthcareLoadingIndicator(message: 'Grounding response in your health baselines...'),
            ),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: const BoxDecoration(
              color: AppTheme.surface,
              border: Border(top: BorderSide(color: AppTheme.cardBorder)),
            ),
            child: SafeArea(
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _inputController,
                      decoration: const InputDecoration(
                        hintText: 'Ask: "Why did my health status change?"...',
                        contentPadding: EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                      ),
                      onSubmitted: (_) => _sendMessage(),
                    ),
                  ),
                  const SizedBox(width: 8),
                  IconButton(
                    style: IconButton.styleFrom(
                      backgroundColor: AppTheme.cyan,
                      foregroundColor: Colors.black,
                    ),
                    icon: const Icon(Icons.send),
                    onPressed: _sendMessage,
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMessageBubble(_CounselorMessageData msg, bool isUser) {
    if (isUser) {
      return Align(
        alignment: Alignment.centerRight,
        child: Container(
          margin: const EdgeInsets.only(bottom: 14),
          constraints: BoxConstraints(maxWidth: MediaQuery.of(context).size.width * 0.8),
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          decoration: BoxDecoration(
            color: AppTheme.indigo,
            borderRadius: BorderRadius.circular(16).copyWith(
              bottomRight: const Radius.circular(2),
            ),
          ),
          child: Text(
            msg.message,
            style: const TextStyle(color: Colors.white, fontSize: 14, height: 1.4),
          ),
        ),
      );
    }

    // Assistant / Counselor Bubble with structured sections
    final isEmergency = msg.isEmergency;
    final borderColor = isEmergency
        ? AppTheme.danger
        : (msg.urgency == 'MONITOR' ? AppTheme.warning : AppTheme.cardBorder);

    return Align(
      alignment: Alignment.centerLeft,
      child: Container(
        margin: const EdgeInsets.only(bottom: 16),
        constraints: BoxConstraints(maxWidth: MediaQuery.of(context).size.width * 0.88),
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: isEmergency ? AppTheme.danger.withValues(alpha: 0.1) : AppTheme.cardBg,
          borderRadius: BorderRadius.circular(16).copyWith(
            bottomLeft: const Radius.circular(2),
          ),
          border: Border.all(color: borderColor, width: isEmergency ? 1.5 : 1.0),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(
                  isEmergency ? Icons.warning_amber_rounded : Icons.psychology_outlined,
                  color: isEmergency ? AppTheme.danger : AppTheme.cyan,
                  size: 18,
                ),
                const SizedBox(width: 6),
                Text(
                  isEmergency ? 'SAFETY PROTOCOL' : 'OS4All Health Counselor',
                  style: TextStyle(
                    color: isEmergency ? AppTheme.danger : AppTheme.cyan,
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const Spacer(),
                HealthcareBadge(
                  label: msg.urgency,
                  color: isEmergency
                      ? AppTheme.danger
                      : (msg.urgency == 'MONITOR' ? AppTheme.warning : AppTheme.success),
                ),
              ],
            ),
            const SizedBox(height: 10),
            Text(
              msg.message,
              style: const TextStyle(color: AppTheme.textPrimary, fontSize: 14, height: 1.45),
            ),

            // 1. OS4All Observations
            if (msg.observations.isNotEmpty) ...[
              const SizedBox(height: 12),
              const Divider(color: AppTheme.cardBorder, height: 1),
              const SizedBox(height: 10),
              _buildSectionHeader('OS4All OBSERVATIONS', Icons.check_circle_outline, AppTheme.cyan),
              ...msg.observations.map((obs) => _buildBulletPoint(obs, AppTheme.textPrimary)),
            ],

            // 2. AI Interpretations
            if (msg.interpretations.isNotEmpty) ...[
              const SizedBox(height: 10),
              _buildSectionHeader('AI INTERPRETATION (NON-DIAGNOSTIC)', Icons.lightbulb_outline, AppTheme.indigo),
              ...msg.interpretations.map((interp) => _buildBulletPoint(interp, AppTheme.textSecondary)),
            ],

            // 3. External Evidence
            if (msg.externalEvidence.isNotEmpty) ...[
              const SizedBox(height: 10),
              _buildSectionHeader('EXTERNAL MEDICAL EVIDENCE', Icons.menu_book_outlined, AppTheme.cyanMuted),
              ...msg.externalEvidence.map((ev) => Container(
                margin: const EdgeInsets.only(top: 6),
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: AppTheme.surfaceLight.withValues(alpha: 0.5),
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: AppTheme.cardBorder),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(ev['title'] ?? '', style: const TextStyle(color: AppTheme.cyan, fontSize: 12, fontWeight: FontWeight.w600)),
                    const SizedBox(height: 2),
                    Text(ev['source'] ?? '', style: const TextStyle(color: AppTheme.textMuted, fontSize: 10)),
                    if (ev['relevance'] != null) ...[
                      const SizedBox(height: 4),
                      Text(ev['relevance']!, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 11)),
                    ],
                  ],
                ),
              )),
            ],

            // 4. Recommended Actions
            if (msg.actions.isNotEmpty) ...[
              const SizedBox(height: 10),
              _buildSectionHeader('RECOMMENDED ACTIONS', Icons.directions_run_outlined, AppTheme.success),
              ...msg.actions.map((act) => _buildBulletPoint(act, AppTheme.textPrimary)),
            ],

            // 5. Professional Evaluation Guidance
            if (msg.evalGuidance != null) ...[
              const SizedBox(height: 10),
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: AppTheme.surfaceLight.withValues(alpha: 0.5),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Icon(Icons.local_hospital_outlined, color: AppTheme.warning, size: 16),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        msg.evalGuidance!,
                        style: const TextStyle(color: AppTheme.textSecondary, fontSize: 11, fontStyle: FontStyle.italic),
                      ),
                    ),
                  ],
                ),
              ),
            ],

            // Mandatory Disclaimer
            if (msg.disclaimer != null) ...[
              const SizedBox(height: 12),
              Text(
                msg.disclaimer!,
                style: const TextStyle(color: AppTheme.textMuted, fontSize: 10, height: 1.3),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildSectionHeader(String title, IconData icon, Color color) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 4),
      child: Row(
        children: [
          Icon(icon, size: 14, color: color),
          const SizedBox(width: 6),
          Text(
            title,
            style: TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.bold, letterSpacing: 0.5),
          ),
        ],
      ),
    );
  }

  Widget _buildBulletPoint(String text, Color textColor) {
    return Padding(
      padding: const EdgeInsets.only(left: 6, top: 3),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('• ', style: TextStyle(color: AppTheme.cyan, fontSize: 12)),
          Expanded(
            child: Text(
              text,
              style: TextStyle(color: textColor, fontSize: 12, height: 1.35),
            ),
          ),
        ],
      ),
    );
  }
}
