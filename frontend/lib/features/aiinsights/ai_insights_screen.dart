import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../core/theme/mobile_design_system.dart';
import '../../shared/widgets/os4_components.dart';
import '../digitaltwin/digital_twin_controller.dart';

/// SCREEN 4 — AI
/// "Gemini explains what the Digital Twin is seeing."
/// Firmly grounds generative intelligence in the deterministic Digital Twin model.
/// Clear Trust Framing: Deterministic Twin -> Authoritative Numbers -> Gemini Explanation.
class AiInsightsScreen extends StatefulWidget {
  const AiInsightsScreen({super.key});

  @override
  State<AiInsightsScreen> createState() => _AiInsightsScreenState();
}

class _AiInsightsScreenState extends State<AiInsightsScreen> {
  final TextEditingController _textController = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  bool _isSending = false;

  @override
  void dispose() {
    _textController.dispose();
    _scrollController.dispose();
    super.dispose();
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

  Future<void> _handleSend(DigitalTwinController controller, String text) async {
    final query = text.trim();
    if (query.isEmpty) return;

    _textController.clear();
    setState(() => _isSending = true);

    try {
      await controller.sendChatMessage(query);
    } finally {
      if (mounted) {
        setState(() => _isSending = false);
        _scrollToBottom();
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final controller = DigitalTwinController();

    return ListenableBuilder(
      listenable: controller,
      builder: (context, _) {
        final explanation = controller.geminiExplanation;
        final headline = explanation['clinicalHeadline']?.toString() ?? 'Metabolic Synthesis';
        final text = explanation['explanation']?.toString() ?? 'Analyzing physiological parameters...';
        final isGemini = explanation['source'] == 'GEMINI';
        final model = explanation['model']?.toString() ?? 'gemini-3.8-flash';
        final isFallback = explanation['isFallback'] == true;
        final factors = controller.contributingFactors;
        final trajectory = controller.trajectoryProjection;
        final projected120 = trajectory['projectedGlucose120Min']?.toString() ?? '332.5';
        final delta = trajectory['projectedDelta120Min']?.toString() ?? '+170.5';

        return Scaffold(
          backgroundColor: MobileTheme.background,
          resizeToAvoidBottomInset: true,
          body: SafeArea(
            child: Column(
              children: [
                Expanded(
                  child: SingleChildScrollView(
                    controller: _scrollController,
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        // 1. Header
                        _buildHeader(),
                        const SizedBox(height: 12),

                        // 2. Grounding Verification Top Badge
                        _buildGroundingTopCard(isGemini, isFallback, model, controller),
                        const SizedBox(height: 14),

                        // 3. Current Situation (AI Summary Card)
                        _buildCurrentSituationCard(headline, text, isFallback, controller),
                        const SizedBox(height: 16),

                        // 4. Why This is Happening (Risk Factors)
                        _buildWhyHappeningSection(factors),
                        const SizedBox(height: 16),

                        // 5. What the Twin Projects (Authoritative Trajectory Numbers)
                        _buildAuthoritativeTrajectoryCard(projected120, delta, controller),
                        const SizedBox(height: 18),

                        // 6. Ask the Digital Twin Chat Area
                        _buildChatSectionHeader(),
                        const SizedBox(height: 10),

                        // Quick Prompts
                        _buildQuickPromptChips(controller),
                        const SizedBox(height: 14),

                        // Chat Messages List
                        _buildChatMessagesList(controller),
                        const SizedBox(height: 16),

                        // Prototype Disclaimer
                        const PrototypeDisclaimerBanner(),
                        const SizedBox(height: 16),
                      ],
                    ),
                  ),
                ),

                // Keyboard-safe anchored bottom chat input bar
                _buildBottomInputBar(controller),
              ],
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
          'AI INSIGHTS',
          style: GoogleFonts.inter(
            fontSize: 22,
            fontWeight: FontWeight.w800,
            color: MobileTheme.textPrimary,
            letterSpacing: -0.4,
          ),
        ),
        const SizedBox(height: 2),
        Text(
          'Gemini explains what the Digital Twin is seeing.',
          style: GoogleFonts.inter(fontSize: 12, color: MobileTheme.textSecondary),
        ),
      ],
    );
  }

  Widget _buildGroundingTopCard(
    bool isGemini,
    bool isFallback,
    String model,
    DigitalTwinController controller,
  ) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      decoration: BoxDecoration(
        color: MobileTheme.surface,
        borderRadius: MobileTheme.cardRadius,
        border: Border.all(
          color: isFallback ? MobileTheme.drift.withOpacity(0.5) : MobileTheme.primary.withOpacity(0.5),
        ),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Expanded(
            child: Row(
              children: [
                Icon(
                  isFallback ? Icons.offline_bolt_rounded : Icons.verified_rounded,
                  color: isFallback ? MobileTheme.drift : MobileTheme.primary,
                  size: 18,
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        isFallback ? 'OFFLINE DETERMINISTIC MODE' : 'GEMINI • GROUNDED IN TWIN',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: GoogleFonts.inter(
                          fontSize: 11,
                          fontWeight: FontWeight.w800,
                          color: isFallback ? MobileTheme.drift : MobileTheme.primary,
                          letterSpacing: 0.4,
                        ),
                      ),
                      Text(
                        isFallback ? 'Fallback heuristic synthesizer' : 'Authoritative deterministic model',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: GoogleFonts.inter(fontSize: 10.5, color: MobileTheme.textSecondary),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          IconButton(
            icon: const Icon(Icons.refresh_rounded, color: MobileTheme.textSecondary, size: 20),
            tooltip: 'Retry Synthesis',
            constraints: const BoxConstraints(minWidth: 48, minHeight: 48),
            onPressed: () => controller.retryGeminiExplanation(),
          ),
        ],
      ),
    );
  }

  Widget _buildCurrentSituationCard(
    String headline,
    String text,
    bool isFallback,
    DigitalTwinController controller,
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
                'CURRENT SITUATION',
                style: GoogleFonts.inter(
                  fontSize: 11,
                  fontWeight: FontWeight.w800,
                  color: MobileTheme.textSecondary,
                  letterSpacing: 0.5,
                ),
              ),
              StatusPill(
                label: 'AI-Generated Explanation',
                color: MobileTheme.geminiPurple,
              ),
            ],
          ),
          const SizedBox(height: 10),
          Text(
            headline,
            style: GoogleFonts.inter(
              fontSize: 16,
              fontWeight: FontWeight.w700,
              color: MobileTheme.textPrimary,
              height: 1.3,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            text,
            style: GoogleFonts.inter(
              fontSize: 13,
              fontWeight: FontWeight.w400,
              color: MobileTheme.textPrimary,
              height: 1.45,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildWhyHappeningSection(List<dynamic> factors) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'WHY THIS IS HAPPENING',
          style: GoogleFonts.inter(
            fontSize: 12,
            fontWeight: FontWeight.w800,
            color: MobileTheme.textPrimary,
            letterSpacing: 0.5,
          ),
        ),
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
            children: factors.map<Widget>((f) {
              final factorMap = f as Map<String, dynamic>;
              final title = factorMap['factor']?.toString() ?? 'Physiological Factor';
              final detail = factorMap['detail']?.toString() ?? '';
              final isIncreasing = factorMap['direction'] == 'RISK_INCREASING';

              return Padding(
                padding: const EdgeInsets.symmetric(vertical: 5),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Icon(
                      isIncreasing ? Icons.warning_amber_rounded : Icons.check_circle_outline_rounded,
                      color: isIncreasing ? MobileTheme.critical : MobileTheme.stable,
                      size: 16,
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            title,
                            style: GoogleFonts.inter(
                              fontSize: 12.5,
                              fontWeight: FontWeight.w700,
                              color: MobileTheme.textPrimary,
                            ),
                          ),
                          if (detail.isNotEmpty) ...[
                            const SizedBox(height: 1),
                            Text(
                              detail,
                              style: GoogleFonts.inter(
                                fontSize: 11,
                                color: MobileTheme.textSecondary,
                              ),
                            ),
                          ],
                        ],
                      ),
                    ),
                  ],
                ),
              );
            }).toList(),
          ),
        ),
      ],
    );
  }

  Widget _buildAuthoritativeTrajectoryCard(
    String projected120,
    String delta,
    DigitalTwinController controller,
  ) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: MobileTheme.surface,
        borderRadius: MobileTheme.cardRadius,
        border: Border.all(color: MobileTheme.primary.withOpacity(0.35)),
      ),
      child: Row(
        children: [
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: MobileTheme.primary.withOpacity(0.15),
              borderRadius: BorderRadius.circular(10),
            ),
            child: const Icon(Icons.analytics_rounded, color: MobileTheme.primary, size: 22),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  'WHAT THE TWIN PROJECTS',
                  style: GoogleFonts.inter(
                    fontSize: 10,
                    fontWeight: FontWeight.w800,
                    color: MobileTheme.primary,
                    letterSpacing: 0.5,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  'At T+120 min: $projected120 mg/dL ($delta)',
                  style: GoogleFonts.inter(
                    fontSize: 13.5,
                    fontWeight: FontWeight.w700,
                    color: MobileTheme.textPrimary,
                  ),
                ),
                Text(
                  'Authoritative numbers generated by deterministic RK4 engine.',
                  style: GoogleFonts.inter(fontSize: 10.5, color: MobileTheme.textSecondary),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildChatSectionHeader() {
    return Row(
      children: [
        const Icon(Icons.chat_bubble_outline_rounded, size: 16, color: MobileTheme.primary),
        const SizedBox(width: 8),
        Text(
          'ASK THE DIGITAL TWIN',
          style: GoogleFonts.inter(
            fontSize: 12,
            fontWeight: FontWeight.w800,
            color: MobileTheme.textPrimary,
            letterSpacing: 0.5,
          ),
        ),
      ],
    );
  }

  Widget _buildQuickPromptChips(DigitalTwinController controller) {
    final prompts = [
      'What is changing?',
      'Why is risk elevated?',
      'What could happen in 2 hours?',
      'What signals matter most?',
    ];

    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: prompts.map((p) {
          return Padding(
            padding: const EdgeInsets.only(right: 8),
            child: ActionChip(
              backgroundColor: MobileTheme.surface,
              side: const BorderSide(color: MobileTheme.border),
              label: Text(
                p,
                style: GoogleFonts.inter(
                  fontSize: 11.5,
                  color: MobileTheme.textPrimary,
                  fontWeight: FontWeight.w500,
                ),
              ),
              onPressed: () => _handleSend(controller, p),
            ),
          );
        }).toList(),
      ),
    );
  }

  Widget _buildChatMessagesList(DigitalTwinController controller) {
    final messages = controller.chatMessages;

    return Column(
      children: messages.map((m) {
        final isUser = m['sender'] == 'user';
        final text = m['text'] ?? '';
        final time = m['time'] ?? 'Just now';
        return ChatBubble(
          message: text,
          isUser: isUser,
          timestamp: time,
        );
      }).toList(),
    );
  }

  Widget _buildBottomInputBar(DigitalTwinController controller) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: const BoxDecoration(
        color: MobileTheme.surface,
        border: Border(top: BorderSide(color: MobileTheme.border, width: 1)),
      ),
      child: Row(
        children: [
          Expanded(
            child: Container(
              constraints: const BoxConstraints(minHeight: 48),
              child: TextField(
                controller: _textController,
                style: GoogleFonts.inter(color: MobileTheme.textPrimary, fontSize: 13.5),
                decoration: InputDecoration(
                  hintText: 'Ask Gemini about physiological signals...',
                  hintStyle: GoogleFonts.inter(color: MobileTheme.textSubtle, fontSize: 12.5),
                  contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                  filled: true,
                  fillColor: MobileTheme.surfaceElevated,
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(24),
                    borderSide: BorderSide.none,
                  ),
                ),
                onSubmitted: (val) => _handleSend(controller, val),
              ),
            ),
          ),
          const SizedBox(width: 8),
          Container(
            width: 44,
            height: 44,
            decoration: const BoxDecoration(
              color: MobileTheme.primary,
              shape: BoxShape.circle,
            ),
            child: IconButton(
              icon: _isSending
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                    )
                  : const Icon(Icons.send_rounded, color: Colors.white, size: 18),
              onPressed: _isSending ? null : () => _handleSend(controller, _textController.text),
            ),
          ),
        ],
      ),
    );
  }
}
