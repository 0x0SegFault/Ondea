# Science behind Ondea

This page lists each rule Ondea uses and the source for it. The code is in [`app/src/main/java/app/ondea/domain/`](../app/src/main/java/app/ondea/domain).

## Timing: what each sign tells you

| Sign | When it appears | What Ondea does with it |
|---|---|---|
| Cervical mucus | Days **before** ovulation | Opens the fertile window. The peak day estimates ovulation. |
| Ovulation test (LH) | 22–56 hours **before** ovulation | Sets the estimate to the day after the first positive test. |
| Temperature rise | **After** ovulation | Confirms ovulation. Teaches Ondea the ovulation day for the next cycles. |

The temperature rise cannot announce ovulation. When it is confirmed, the fertile days of that cycle have passed. Its value is for the next cycles: after one confirmed cycle, the prediction moves to your own ovulation day.

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

## Limits

- Ondea is not a medical device and not a contraceptive.
- A prediction is an estimate. Only the signs of the current cycle show what happens in that cycle.
- Illness, alcohol, short nights and late measurements change the temperature. Mark these readings as disturbed.
