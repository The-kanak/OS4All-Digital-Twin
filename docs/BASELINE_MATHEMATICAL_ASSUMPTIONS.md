# OS4All Personal Baseline Engine: Mathematical Assumptions & Statistical Formulation

> **Fundamental Principle:** *"Normal for the population is not necessarily normal for the individual."*

The OS4All Personal Baseline Engine computes individual-specific baselines rather than enforcing generic, static population intervals. Statistical deviations describe departures from a user's personal longitudinal patterns and **do not constitute medical diagnoses**.

---

## 1. Mathematical Formulation & Assumptions

### 1.1 Sample Adequacy Criterion ($N \ge N_{\text{min}}$)
To prevent erratic or misleading baselines generated from one or two sparse readings, baseline establishment requires a minimum statistical sample size:
- Standard continuous metrics (Heart Rate, Resting Heart Rate, HRV, SpO2, Steps, Sleep Duration, Temperature): **$N_{\text{min}} = 5$ valid observations** within the evaluation window (typically 30–90 days).
- If $N < N_{\text{min}}$, the baseline state is marked `baselineEstablished = false` with informative guidance on how many additional data points are required before a statistically sound baseline can be derived.

---

### 1.2 Missing Value & Null Policy
- Any observation where the numerical metric value is missing (`null`), `NaN`, or infinite is strictly excluded from sample vectors prior to statistical computation.
- Observations with negative durations or non-physical biometric figures (e.g., negative heart rate or body temperatures outside $[25^\circ\text{C}, 45^\circ\text{C}]$) are filtered out as sensor noise.

---

### 1.3 Outlier Robustness & Filtering (Tukey's Fences / Median Absolute Deviation)
Sensor artifacts and motion noise can skew small sample estimations.
- **Interquartile Range (IQR):**
  $$IQR = Q_3 - Q_1$$
- Observations falling beyond Tukey's inner fences $[Q_1 - 1.5 \cdot IQR, Q_3 + 1.5 \cdot IQR]$ are classified as statistical outliers.
- The Engine calculates the robust **Median** ($Q_2$) alongside the sample mean to preserve central tendency even when extreme noise points exist.

---

### 1.4 Central Tendency and Variance Estimators

#### Sample Mean ($\bar{x}$)
$$\bar{x} = \frac{1}{N} \sum_{i=1}^N x_i$$

#### Sample Median ($\tilde{x}$)
Given values sorted in ascending order $x_{(1)} \le x_{(2)} \le \dots \le x_{(N)}$:
$$\tilde{x} = \begin{cases} x_{\left(\frac{N+1}{2}\right)} & \text{if } N \text{ is odd} \\ \frac{x_{\left(\frac{N}{2}\right)} + x_{\left(\frac{N}{2} + 1\right)}}{2} & \text{if } N \text{ is even} \end{cases}$$

#### Sample Standard Deviation ($s$)
Using Bessel's correction for unbiased sample variance ($N - 1$ degrees of freedom):
$$s = \sqrt{\frac{1}{N - 1} \sum_{i=1}^N (x_i - \bar{x})^2}$$
*Degeneracy guard:* If all historical values are identical, sample variance $s^2 = 0$. In this case, standard deviation is zero, and $z$-score is undefined / null (avoiding division by zero).

---

### 1.5 Personal Baseline Range
The expected personal operating envelope is calculated using a $1.96\sigma$ parametric confidence interval (spanning approximately 95% of the user's typical physiological variability):
$$\text{Baseline Low} = \bar{x} - 1.96 \cdot s$$
$$\text{Baseline High} = \bar{x} + 1.96 \cdot s$$
*(Physiological lower limits are clamped to zero where negative values are impossible, such as heart rate or steps).*

---

### 1.6 Recent Average & Longitudinal Departure

#### Recent Average ($\bar{x}_{\text{recent}}$)
Calculated over the most recent $k = \min(3, N)$ observations:
$$\bar{x}_{\text{recent}} = \frac{1}{k} \sum_{i=N-k+1}^N x_i$$

#### Absolute and Relative Deviation
$$\Delta_{\text{abs}} = \bar{x}_{\text{recent}} - \bar{x}$$
$$\Delta_{\%} = \left( \frac{\bar{x}_{\text{recent}} - \bar{x}}{\bar{x}} \right) \times 100\% \quad (\text{for } \bar{x} \ne 0)$$

---

### 1.7 Standardized $z$-Score
The $z$-score quantifies how many standard deviations the recent average deviates from the user's historical distribution:
$$z = \frac{\bar{x}_{\text{recent}} - \bar{x}}{s}$$

**Statistical Guardrails for $z$-Score Applicability:**
1. $z$-score is **only computed** when $N \ge N_{\text{min}}$ and $s > 0$.
2. If $s = 0$ (no variance in history), $z$-score is omitted (`null`).
3. If $N < 5$, $z$-score is omitted (`null`).

---

### 1.8 Longitudinal Trend Determination
Longitudinal slope is computed across the observation sequence using Ordinary Least Squares (OLS) regression or recent-to-baseline difference:
$$\text{Slope } \beta = \frac{\sum (t_i - \bar{t})(x_i - \bar{x})}{\sum (t_i - \bar{t})^2}$$
- **`INCREASING`**: $z \ge +1.0$ or $\Delta_{\%} > +7.5\%$
- **`DECREASING`**: $z \le -1.0$ or $\Delta_{\%} < -7.5\%$
- **`STABLE`**: $-1.0 < z < +1.0$

---

## 2. Clinical Non-Diagnostic Disclaimer
> **CRITICAL CLINICAL SAFETY NOTICE:**
> Statistical deviation from personal baseline **DOES NOT** indicate clinical disease, pathology, or diagnosis. A change in resting heart rate or HRV can be triggered by exercise adaptation, altitude changes, travel, hydration, psychological stress, or sleep variations. OS4All models provide personalized situational awareness, never automated diagnoses.
