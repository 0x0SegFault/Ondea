# Science behind Ondea

This page lists each rule Ondea uses and the source for it. The code is in [`app/src/main/java/app/ondea/domain/`](../app/src/main/java/app/ondea/domain).

## Timing: what each sign tells you

| Sign | When it appears | What Ondea does with it |
|---|---|---|
| Cervical mucus | Days **before** ovulation | Opens the fertile window. Creamy and egg-white mucus move the estimate day by day. The peak day estimates ovulation. |
| Ovulation test (LH) | 22–56 hours **before** ovulation | Sets the estimate to the day after the first positive test. |
| Temperature rise | **After** ovulation, 1 to 1.5 days after progesterone goes up | Shown as "rising" from the first high reading, confirmed after 3. Teaches Ondea the ovulation day for the next cycles. |

The temperature rise cannot announce ovulation. When it is confirmed, the fertile days of that cycle have passed. Its value is for the next cycles.

## The forecast day and what Ondea learns

- The forecast uses **one ovulation day**: the day set by the user, or a typical cycle (cycle length minus a 14-day luteal phase). Only the user changes it.
- After a finished cycle with a sign, Ondea compares what the signs show with that day. When they differ by 2 days or more, Ondea suggests the new day. The user chooses.
- Why Ondea suggests and does not change the day alone: the ovulation day varies between cycles of the same person, mostly because the phase before ovulation varies (Fehring 2006; Bull 2019). A change of 1 day is within that normal variation.
- Within a cycle, the signs of that cycle move its estimate day by day. They do not change the forecast day.

## Rules and sources

| Rule in Ondea | Source |
|---|---|
| Ovulation is confirmed when 3 readings in a row are above the highest of the 6 before (the coverline), and the 3rd is at least 0.2 °C above it. If not, a 4th reading above the coverline confirms. Disturbed readings are skipped. | Sensiplan (Arbeitsgruppe NFP, *Natürlich und sicher*, TRIAS); Frank-Herrmann P. et al., [*Hum Reprod* 2007;22:1310–19](https://doi.org/10.1093/humrep/dem003) |
| Ovulation is estimated on the day before the first high reading. | Ecochard R. et al., [*BJOG* 2001;108:822–29](https://doi.org/10.1111/j.1471-0528.2001.00194.x) |
| The mucus peak is the last day of the best mucus. The fertile phase ends only when the temperature rise is confirmed **and** 3 days have passed since the peak (double check). | Sensiplan; Frank-Herrmann 2007; Ecochard 2001 |
| Ovulation follows the start of the LH surge by 22 to 56 hours (mean 34 h), so 1 to 2 days after the first positive test. This varies between people. Ondea sets ovulation on the day after the first positive test and ends the window 2 days after it. | Erden M. et al., [*Hum Reprod Update* 2022;28:717–32](https://doi.org/10.1093/humupd/dmac012); Direito A. et al., [*Fertil Steril* 2013;99:279–85](https://doi.org/10.1016/j.fertnstert.2012.08.047) |
| About 7 % of positive home LH tests are not followed by ovulation (up to 18 % with one test a day). About half of LH surges are short spikes that one test a day can miss. So a positive test is an estimate, and the temperature rise always overrides it. | McGovern P.G. et al., [*Fertil Steril* 2004;82:1273–77](https://doi.org/10.1016/j.fertnstert.2004.03.070); Erden 2022 |
| The fertile window is the 5 days before ovulation, ovulation day and the day after. Conception chance per day: 10, 16, 14, 27, 31 and 33 % from 5 days before ovulation to ovulation day. | Wilcox A.J. et al., [*N Engl J Med* 1995;333:1517–21](https://doi.org/10.1056/NEJM199512073332301) |
| The predicted window opens 5 days before the earliest recent ovulation, because the fertile window changes from cycle to cycle, even in regular cycles. | Wilcox A.J. et al., [*BMJ* 2000;321:1259–62](https://doi.org/10.1136/bmj.321.7271.1259) |
| Ovulation day is learned per person (average of the last 6 confirmed cycles). The luteal phase is learned too, 14 days by default. | Fehring R.J. et al., [*JOGNN* 2006;35:376–84](https://doi.org/10.1111/j.1552-6909.2006.00051.x); Bull J.R. et al., [*npj Digit Med* 2019;2:83](https://doi.org/10.1038/s41746-019-0152-7) |
| 18 or more days of high temperature can be a sign of pregnancy. | Sensiplan |
| The temperature is entered to 0.01 °C (0.02 °F), as a basal thermometer shows it. After ovulation the temperature is about 0.3 to 0.7 °C higher, so a fever thermometer (0.1 °C) is not precise enough. | Baker F.C. et al., [*Temperature* 2020;7:226–62](https://doi.org/10.1080/23328940.2020.1735927) |
| The temperature rise follows the progesterone rise by 24 to 36 hours. This is why ovulation is set on the day before the first high reading. | [*Fertil Steril* 1984;41:254–59](https://pubmed.ncbi.nlm.nih.gov/6421622/); Ecochard 2001 |
| **Rising, not confirmed:** 1 or 2 readings above the coverline of the 6 before. A single high reading counts only if it is at least 0.1 °C above. Shown as an estimate until the 3-over-6 rule confirms it. | Ondea's choice, based on the Sensiplan rule; not a separate study |
| **Mucus moves the estimate:** egg-white mucus sets the estimate to that day, creamy mucus to the next day, plus the user's usual gap (see below). Mucus on bleeding days is ignored, because it cannot be observed then. | Hilgers T.W. et al., *Obstet Gynecol* 1978;52:575–82 ([PubMed](https://pubmed.ncbi.nlm.nih.gov/724176/)): ovulation was within 2 days of the mucus peak in 95 % of cycles, on average 0.3 days before it; Sensiplan for the mucus categories |
| **Late ovulation:** when the forecast day has passed without a sign and the user still records temperature or mucus, the estimate moves one day at a time, at most 10 days. | The phase before ovulation is the most variable part of the cycle (Bull 2019: 16.9 ± 5.3 days). The 10-day cap is Ondea's choice. |
| **What Ondea learns from a finished cycle:** the ovulation day from the temperature rise, else the day after a positive LH test, else the mucus peak; the user's usual low and high temperature; the usual gap between mucus peak and ovulation (median of the last 6 cycles with both signs, limited to −2…+2 days). | Ecochard 2001 (the mucus peak and the start of the LH rise are close to ovulation on ultrasound in more than 72 % of cycles); Hilgers 1978 |
| **Do the signs agree?** In the current cycle, when there is a temperature rise and a mucus peak, Ondea compares the two ovulation days. 2 days apart or less: they agree. 3 or more: Ondea asks the user to check for disturbed readings. | Hilgers 1978 (ovulation within ±2 days of the peak in 95 % of cycles); the Sensiplan double check (Frank-Herrmann 2007) |
| The typical curve on the chart is a drawing of the usual shape (low before ovulation, higher after), not anyone's data. | Baker 2020 |

## Limits

- Ondea is not a medical device and not a contraceptive.
- A prediction is an estimate. Only the signs of the current cycle show what happens in that cycle.
- Illness, alcohol, short nights and late measurements change the temperature. Mark these readings as disturbed.
