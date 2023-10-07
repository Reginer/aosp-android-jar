/*
 * Copyright (C) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package benchmarks;

public class CloneBenchmark {
    static class CloneableObject implements Cloneable {
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    static class CloneableManyFieldObject implements Cloneable {
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        Object o1 = new Object();
        Object o2 = new Object();
        Object o3 = new Object();
        Object o4 = new Object();
        Object o5 = new Object();
        Object o6 = new Object();
        Object o7 = new Object();
        Object o8 = new Object();
        Object o9 = new Object();
        Object o10 = new Object();
        Object o11 = new Object();
        Object o12 = new Object();
        Object o13 = new Object();
        Object o14 = new Object();
        Object o15 = new Object();
        Object o16 = new Object();
        Object o17 = new Object();
        Object o18 = new Object();
        Object o19 = new Object();
        Object o20 = new Object();
        Object o21 = new Object();
        Object o22 = new Object();
        Object o23 = new Object();
        Object o24 = new Object();
        Object o25 = new Object();
        Object o26 = new Object();
        Object o27 = new Object();
        Object o28 = new Object();
        Object o29 = new Object();
        Object o30 = new Object();
        Object o31 = new Object();
        Object o32 = new Object();
        Object o33 = new Object();
        Object o34 = new Object();
        Object o35 = new Object();
        Object o36 = new Object();
        Object o37 = new Object();
        Object o38 = new Object();
        Object o39 = new Object();
        Object o40 = new Object();
        Object o41 = new Object();
        Object o42 = new Object();
        Object o43 = new Object();
        Object o44 = new Object();
        Object o45 = new Object();
        Object o46 = new Object();
        Object o47 = new Object();
        Object o48 = new Object();
        Object o49 = new Object();
        Object o50 = new Object();
        Object o51 = new Object();
        Object o52 = new Object();
        Object o53 = new Object();
        Object o54 = new Object();
        Object o55 = new Object();
        Object o56 = new Object();
        Object o57 = new Object();
        Object o58 = new Object();
        Object o59 = new Object();
        Object o60 = new Object();
        Object o61 = new Object();
        Object o62 = new Object();
        Object o63 = new Object();
        Object o64 = new Object();
        Object o65 = new Object();
        Object o66 = new Object();
        Object o67 = new Object();
        Object o68 = new Object();
        Object o69 = new Object();
        Object o70 = new Object();
        Object o71 = new Object();
        Object o72 = new Object();
        Object o73 = new Object();
        Object o74 = new Object();
        Object o75 = new Object();
        Object o76 = new Object();
        Object o77 = new Object();
        Object o78 = new Object();
        Object o79 = new Object();
        Object o80 = new Object();
        Object o81 = new Object();
        Object o82 = new Object();
        Object o83 = new Object();
        Object o84 = new Object();
        Object o85 = new Object();
        Object o86 = new Object();
        Object o87 = new Object();
        Object o88 = new Object();
        Object o89 = new Object();
        Object o90 = new Object();
        Object o91 = new Object();
        Object o92 = new Object();
        Object o93 = new Object();
        Object o94 = new Object();
        Object o95 = new Object();
        Object o96 = new Object();
        Object o97 = new Object();
        Object o98 = new Object();
        Object o99 = new Object();
        Object o100 = new Object();
        Object o101 = new Object();
        Object o102 = new Object();
        Object o103 = new Object();
        Object o104 = new Object();
        Object o105 = new Object();
        Object o106 = new Object();
        Object o107 = new Object();
        Object o108 = new Object();
        Object o109 = new Object();
        Object o110 = new Object();
        Object o111 = new Object();
        Object o112 = new Object();
        Object o113 = new Object();
        Object o114 = new Object();
        Object o115 = new Object();
        Object o116 = new Object();
        Object o117 = new Object();
        Object o118 = new Object();
        Object o119 = new Object();
        Object o120 = new Object();
        Object o121 = new Object();
        Object o122 = new Object();
        Object o123 = new Object();
        Object o124 = new Object();
        Object o125 = new Object();
        Object o126 = new Object();
        Object o127 = new Object();
        Object o128 = new Object();
        Object o129 = new Object();
        Object o130 = new Object();
        Object o131 = new Object();
        Object o132 = new Object();
        Object o133 = new Object();
        Object o134 = new Object();
        Object o135 = new Object();
        Object o136 = new Object();
        Object o137 = new Object();
        Object o138 = new Object();
        Object o139 = new Object();
        Object o140 = new Object();
        Object o141 = new Object();
        Object o142 = new Object();
        Object o143 = new Object();
        Object o144 = new Object();
        Object o145 = new Object();
        Object o146 = new Object();
        Object o147 = new Object();
        Object o148 = new Object();
        Object o149 = new Object();
        Object o150 = new Object();
        Object o151 = new Object();
        Object o152 = new Object();
        Object o153 = new Object();
        Object o154 = new Object();
        Object o155 = new Object();
        Object o156 = new Object();
        Object o157 = new Object();
        Object o158 = new Object();
        Object o159 = new Object();
        Object o160 = new Object();
        Object o161 = new Object();
        Object o162 = new Object();
        Object o163 = new Object();
        Object o164 = new Object();
        Object o165 = new Object();
        Object o166 = new Object();
        Object o167 = new Object();
        Object o168 = new Object();
        Object o169 = new Object();
        Object o170 = new Object();
        Object o171 = new Object();
        Object o172 = new Object();
        Object o173 = new Object();
        Object o174 = new Object();
        Object o175 = new Object();
        Object o176 = new Object();
        Object o177 = new Object();
        Object o178 = new Object();
        Object o179 = new Object();
        Object o180 = new Object();
        Object o181 = new Object();
        Object o182 = new Object();
        Object o183 = new Object();
        Object o184 = new Object();
        Object o185 = new Object();
        Object o186 = new Object();
        Object o187 = new Object();
        Object o188 = new Object();
        Object o189 = new Object();
        Object o190 = new Object();
        Object o191 = new Object();
        Object o192 = new Object();
        Object o193 = new Object();
        Object o194 = new Object();
        Object o195 = new Object();
        Object o196 = new Object();
        Object o197 = new Object();
        Object o198 = new Object();
        Object o199 = new Object();
        Object o200 = new Object();
        Object o201 = new Object();
        Object o202 = new Object();
        Object o203 = new Object();
        Object o204 = new Object();
        Object o205 = new Object();
        Object o206 = new Object();
        Object o207 = new Object();
        Object o208 = new Object();
        Object o209 = new Object();
        Object o210 = new Object();
        Object o211 = new Object();
        Object o212 = new Object();
        Object o213 = new Object();
        Object o214 = new Object();
        Object o215 = new Object();
        Object o216 = new Object();
        Object o217 = new Object();
        Object o218 = new Object();
        Object o219 = new Object();
        Object o220 = new Object();
        Object o221 = new Object();
        Object o222 = new Object();
        Object o223 = new Object();
        Object o224 = new Object();
        Object o225 = new Object();
        Object o226 = new Object();
        Object o227 = new Object();
        Object o228 = new Object();
        Object o229 = new Object();
        Object o230 = new Object();
        Object o231 = new Object();
        Object o232 = new Object();
        Object o233 = new Object();
        Object o234 = new Object();
        Object o235 = new Object();
        Object o236 = new Object();
        Object o237 = new Object();
        Object o238 = new Object();
        Object o239 = new Object();
        Object o240 = new Object();
        Object o241 = new Object();
        Object o242 = new Object();
        Object o243 = new Object();
        Object o244 = new Object();
        Object o245 = new Object();
        Object o246 = new Object();
        Object o247 = new Object();
        Object o248 = new Object();
        Object o249 = new Object();
        Object o250 = new Object();
        Object o251 = new Object();
        Object o252 = new Object();
        Object o253 = new Object();
        Object o254 = new Object();
        Object o255 = new Object();
        Object o256 = new Object();
        Object o257 = new Object();
        Object o258 = new Object();
        Object o259 = new Object();
        Object o260 = new Object();
        Object o261 = new Object();
        Object o262 = new Object();
        Object o263 = new Object();
        Object o264 = new Object();
        Object o265 = new Object();
        Object o266 = new Object();
        Object o267 = new Object();
        Object o268 = new Object();
        Object o269 = new Object();
        Object o270 = new Object();
        Object o271 = new Object();
        Object o272 = new Object();
        Object o273 = new Object();
        Object o274 = new Object();
        Object o275 = new Object();
        Object o276 = new Object();
        Object o277 = new Object();
        Object o278 = new Object();
        Object o279 = new Object();
        Object o280 = new Object();
        Object o281 = new Object();
        Object o282 = new Object();
        Object o283 = new Object();
        Object o284 = new Object();
        Object o285 = new Object();
        Object o286 = new Object();
        Object o287 = new Object();
        Object o288 = new Object();
        Object o289 = new Object();
        Object o290 = new Object();
        Object o291 = new Object();
        Object o292 = new Object();
        Object o293 = new Object();
        Object o294 = new Object();
        Object o295 = new Object();
        Object o296 = new Object();
        Object o297 = new Object();
        Object o298 = new Object();
        Object o299 = new Object();
        Object o300 = new Object();
        Object o301 = new Object();
        Object o302 = new Object();
        Object o303 = new Object();
        Object o304 = new Object();
        Object o305 = new Object();
        Object o306 = new Object();
        Object o307 = new Object();
        Object o308 = new Object();
        Object o309 = new Object();
        Object o310 = new Object();
        Object o311 = new Object();
        Object o312 = new Object();
        Object o313 = new Object();
        Object o314 = new Object();
        Object o315 = new Object();
        Object o316 = new Object();
        Object o317 = new Object();
        Object o318 = new Object();
        Object o319 = new Object();
        Object o320 = new Object();
        Object o321 = new Object();
        Object o322 = new Object();
        Object o323 = new Object();
        Object o324 = new Object();
        Object o325 = new Object();
        Object o326 = new Object();
        Object o327 = new Object();
        Object o328 = new Object();
        Object o329 = new Object();
        Object o330 = new Object();
        Object o331 = new Object();
        Object o332 = new Object();
        Object o333 = new Object();
        Object o334 = new Object();
        Object o335 = new Object();
        Object o336 = new Object();
        Object o337 = new Object();
        Object o338 = new Object();
        Object o339 = new Object();
        Object o340 = new Object();
        Object o341 = new Object();
        Object o342 = new Object();
        Object o343 = new Object();
        Object o344 = new Object();
        Object o345 = new Object();
        Object o346 = new Object();
        Object o347 = new Object();
        Object o348 = new Object();
        Object o349 = new Object();
        Object o350 = new Object();
        Object o351 = new Object();
        Object o352 = new Object();
        Object o353 = new Object();
        Object o354 = new Object();
        Object o355 = new Object();
        Object o356 = new Object();
        Object o357 = new Object();
        Object o358 = new Object();
        Object o359 = new Object();
        Object o360 = new Object();
        Object o361 = new Object();
        Object o362 = new Object();
        Object o363 = new Object();
        Object o364 = new Object();
        Object o365 = new Object();
        Object o366 = new Object();
        Object o367 = new Object();
        Object o368 = new Object();
        Object o369 = new Object();
        Object o370 = new Object();
        Object o371 = new Object();
        Object o372 = new Object();
        Object o373 = new Object();
        Object o374 = new Object();
        Object o375 = new Object();
        Object o376 = new Object();
        Object o377 = new Object();
        Object o378 = new Object();
        Object o379 = new Object();
        Object o380 = new Object();
        Object o381 = new Object();
        Object o382 = new Object();
        Object o383 = new Object();
        Object o384 = new Object();
        Object o385 = new Object();
        Object o386 = new Object();
        Object o387 = new Object();
        Object o388 = new Object();
        Object o389 = new Object();
        Object o390 = new Object();
        Object o391 = new Object();
        Object o392 = new Object();
        Object o393 = new Object();
        Object o394 = new Object();
        Object o395 = new Object();
        Object o396 = new Object();
        Object o397 = new Object();
        Object o398 = new Object();
        Object o399 = new Object();
        Object o400 = new Object();
        Object o401 = new Object();
        Object o402 = new Object();
        Object o403 = new Object();
        Object o404 = new Object();
        Object o405 = new Object();
        Object o406 = new Object();
        Object o407 = new Object();
        Object o408 = new Object();
        Object o409 = new Object();
        Object o410 = new Object();
        Object o411 = new Object();
        Object o412 = new Object();
        Object o413 = new Object();
        Object o414 = new Object();
        Object o415 = new Object();
        Object o416 = new Object();
        Object o417 = new Object();
        Object o418 = new Object();
        Object o419 = new Object();
        Object o420 = new Object();
        Object o421 = new Object();
        Object o422 = new Object();
        Object o423 = new Object();
        Object o424 = new Object();
        Object o425 = new Object();
        Object o426 = new Object();
        Object o427 = new Object();
        Object o428 = new Object();
        Object o429 = new Object();
        Object o430 = new Object();
        Object o431 = new Object();
        Object o432 = new Object();
        Object o433 = new Object();
        Object o434 = new Object();
        Object o435 = new Object();
        Object o436 = new Object();
        Object o437 = new Object();
        Object o438 = new Object();
        Object o439 = new Object();
        Object o440 = new Object();
        Object o441 = new Object();
        Object o442 = new Object();
        Object o460 = new Object();
        Object o461 = new Object();
        Object o462 = new Object();
        Object o463 = new Object();
        Object o464 = new Object();
        Object o465 = new Object();
        Object o466 = new Object();
        Object o467 = new Object();
        Object o468 = new Object();
        Object o469 = new Object();
        Object o470 = new Object();
        Object o471 = new Object();
        Object o472 = new Object();
        Object o473 = new Object();
        Object o474 = new Object();
        Object o475 = new Object();
        Object o476 = new Object();
        Object o477 = new Object();
        Object o478 = new Object();
        Object o479 = new Object();
        Object o480 = new Object();
        Object o481 = new Object();
        Object o482 = new Object();
        Object o483 = new Object();
        Object o484 = new Object();
        Object o485 = new Object();
        Object o486 = new Object();
        Object o487 = new Object();
        Object o488 = new Object();
        Object o489 = new Object();
        Object o490 = new Object();
        Object o491 = new Object();
        Object o492 = new Object();
        Object o493 = new Object();
        Object o494 = new Object();
        Object o495 = new Object();
        Object o496 = new Object();
        Object o497 = new Object();
        Object o498 = new Object();
        Object o499 = new Object();
        Object o500 = new Object();
        Object o501 = new Object();
        Object o502 = new Object();
        Object o503 = new Object();
        Object o504 = new Object();
        Object o505 = new Object();
        Object o506 = new Object();
        Object o507 = new Object();
        Object o508 = new Object();
        Object o509 = new Object();
        Object o510 = new Object();
        Object o511 = new Object();
        Object o512 = new Object();
        Object o513 = new Object();
        Object o514 = new Object();
        Object o515 = new Object();
        Object o516 = new Object();
        Object o517 = new Object();
        Object o518 = new Object();
        Object o519 = new Object();
        Object o520 = new Object();
        Object o521 = new Object();
        Object o522 = new Object();
        Object o523 = new Object();
        Object o556 = new Object();
        Object o557 = new Object();
        Object o558 = new Object();
        Object o559 = new Object();
        Object o560 = new Object();
        Object o561 = new Object();
        Object o562 = new Object();
        Object o563 = new Object();
        Object o564 = new Object();
        Object o565 = new Object();
        Object o566 = new Object();
        Object o567 = new Object();
        Object o568 = new Object();
        Object o569 = new Object();
        Object o570 = new Object();
        Object o571 = new Object();
        Object o572 = new Object();
        Object o573 = new Object();
        Object o574 = new Object();
        Object o575 = new Object();
        Object o576 = new Object();
        Object o577 = new Object();
        Object o578 = new Object();
        Object o579 = new Object();
        Object o580 = new Object();
        Object o581 = new Object();
        Object o582 = new Object();
        Object o583 = new Object();
        Object o584 = new Object();
        Object o585 = new Object();
        Object o586 = new Object();
        Object o587 = new Object();
        Object o588 = new Object();
        Object o589 = new Object();
        Object o590 = new Object();
        Object o591 = new Object();
        Object o592 = new Object();
        Object o593 = new Object();
        Object o594 = new Object();
        Object o595 = new Object();
        Object o596 = new Object();
        Object o597 = new Object();
        Object o598 = new Object();
        Object o599 = new Object();
        Object o600 = new Object();
        Object o601 = new Object();
        Object o602 = new Object();
        Object o603 = new Object();
        Object o604 = new Object();
        Object o605 = new Object();
        Object o606 = new Object();
        Object o607 = new Object();
        Object o608 = new Object();
        Object o609 = new Object();
        Object o610 = new Object();
        Object o611 = new Object();
        Object o612 = new Object();
        Object o613 = new Object();
        Object o614 = new Object();
        Object o615 = new Object();
        Object o616 = new Object();
        Object o617 = new Object();
        Object o618 = new Object();
        Object o619 = new Object();
        Object o620 = new Object();
        Object o621 = new Object();
        Object o622 = new Object();
        Object o623 = new Object();
        Object o624 = new Object();
        Object o625 = new Object();
        Object o626 = new Object();
        Object o627 = new Object();
        Object o628 = new Object();
        Object o629 = new Object();
        Object o630 = new Object();
        Object o631 = new Object();
        Object o632 = new Object();
        Object o633 = new Object();
        Object o634 = new Object();
        Object o635 = new Object();
        Object o636 = new Object();
        Object o637 = new Object();
        Object o638 = new Object();
        Object o639 = new Object();
        Object o640 = new Object();
        Object o641 = new Object();
        Object o642 = new Object();
        Object o643 = new Object();
        Object o644 = new Object();
        Object o645 = new Object();
        Object o646 = new Object();
        Object o647 = new Object();
        Object o648 = new Object();
        Object o649 = new Object();
        Object o650 = new Object();
        Object o651 = new Object();
        Object o652 = new Object();
        Object o653 = new Object();
        Object o654 = new Object();
        Object o655 = new Object();
        Object o656 = new Object();
        Object o657 = new Object();
        Object o658 = new Object();
        Object o659 = new Object();
        Object o660 = new Object();
        Object o661 = new Object();
        Object o662 = new Object();
        Object o663 = new Object();
        Object o664 = new Object();
        Object o665 = new Object();
        Object o666 = new Object();
        Object o667 = new Object();
        Object o668 = new Object();
        Object o669 = new Object();
        Object o670 = new Object();
        Object o671 = new Object();
        Object o672 = new Object();
        Object o673 = new Object();
        Object o674 = new Object();
        Object o675 = new Object();
        Object o676 = new Object();
        Object o677 = new Object();
        Object o678 = new Object();
        Object o679 = new Object();
        Object o680 = new Object();
        Object o681 = new Object();
        Object o682 = new Object();
        Object o683 = new Object();
        Object o684 = new Object();
        Object o685 = new Object();
        Object o686 = new Object();
        Object o687 = new Object();
        Object o688 = new Object();
        Object o734 = new Object();
        Object o735 = new Object();
        Object o736 = new Object();
        Object o737 = new Object();
        Object o738 = new Object();
        Object o739 = new Object();
        Object o740 = new Object();
        Object o741 = new Object();
        Object o742 = new Object();
        Object o743 = new Object();
        Object o744 = new Object();
        Object o745 = new Object();
        Object o746 = new Object();
        Object o747 = new Object();
        Object o748 = new Object();
        Object o749 = new Object();
        Object o750 = new Object();
        Object o751 = new Object();
        Object o752 = new Object();
        Object o753 = new Object();
        Object o754 = new Object();
        Object o755 = new Object();
        Object o756 = new Object();
        Object o757 = new Object();
        Object o758 = new Object();
        Object o759 = new Object();
        Object o760 = new Object();
        Object o761 = new Object();
        Object o762 = new Object();
        Object o763 = new Object();
        Object o764 = new Object();
        Object o765 = new Object();
        Object o766 = new Object();
        Object o767 = new Object();
        Object o768 = new Object();
        Object o769 = new Object();
        Object o770 = new Object();
        Object o771 = new Object();
        Object o772 = new Object();
        Object o773 = new Object();
        Object o774 = new Object();
        Object o775 = new Object();
        Object o776 = new Object();
        Object o777 = new Object();
        Object o778 = new Object();
        Object o779 = new Object();
        Object o780 = new Object();
        Object o781 = new Object();
        Object o782 = new Object();
        Object o783 = new Object();
        Object o784 = new Object();
        Object o785 = new Object();
        Object o786 = new Object();
        Object o787 = new Object();
        Object o788 = new Object();
        Object o789 = new Object();
        Object o790 = new Object();
        Object o791 = new Object();
        Object o792 = new Object();
        Object o793 = new Object();
        Object o794 = new Object();
        Object o795 = new Object();
        Object o796 = new Object();
        Object o797 = new Object();
        Object o798 = new Object();
        Object o799 = new Object();
        Object o800 = new Object();
        Object o801 = new Object();
        Object o802 = new Object();
        Object o803 = new Object();
        Object o804 = new Object();
        Object o805 = new Object();
        Object o806 = new Object();
        Object o807 = new Object();
        Object o808 = new Object();
        Object o809 = new Object();
        Object o810 = new Object();
        Object o811 = new Object();
        Object o812 = new Object();
        Object o813 = new Object();
        Object o848 = new Object();
        Object o849 = new Object();
        Object o850 = new Object();
        Object o851 = new Object();
        Object o852 = new Object();
        Object o853 = new Object();
        Object o854 = new Object();
        Object o855 = new Object();
        Object o856 = new Object();
        Object o857 = new Object();
        Object o858 = new Object();
        Object o859 = new Object();
        Object o860 = new Object();
        Object o861 = new Object();
        Object o862 = new Object();
        Object o863 = new Object();
        Object o864 = new Object();
        Object o865 = new Object();
        Object o866 = new Object();
        Object o867 = new Object();
        Object o868 = new Object();
        Object o869 = new Object();
        Object o870 = new Object();
        Object o871 = new Object();
        Object o872 = new Object();
        Object o873 = new Object();
        Object o874 = new Object();
        Object o875 = new Object();
        Object o876 = new Object();
        Object o877 = new Object();
        Object o878 = new Object();
        Object o879 = new Object();
        Object o880 = new Object();
        Object o881 = new Object();
        Object o882 = new Object();
        Object o883 = new Object();
        Object o884 = new Object();
        Object o885 = new Object();
        Object o886 = new Object();
        Object o887 = new Object();
        Object o888 = new Object();
        Object o889 = new Object();
        Object o890 = new Object();
        Object o891 = new Object();
        Object o892 = new Object();
        Object o893 = new Object();
        Object o894 = new Object();
        Object o895 = new Object();
        Object o896 = new Object();
        Object o897 = new Object();
        Object o898 = new Object();
        Object o899 = new Object();
        Object o900 = new Object();
        Object o901 = new Object();
        Object o902 = new Object();
        Object o903 = new Object();
        Object o904 = new Object();
        Object o905 = new Object();
        Object o906 = new Object();
        Object o907 = new Object();
        Object o908 = new Object();
        Object o909 = new Object();
        Object o910 = new Object();
        Object o911 = new Object();
        Object o912 = new Object();
        Object o913 = new Object();
        Object o914 = new Object();
        Object o915 = new Object();
        Object o916 = new Object();
        Object o917 = new Object();
        Object o918 = new Object();
        Object o919 = new Object();
        Object o920 = new Object();
        Object o921 = new Object();
        Object o922 = new Object();
        Object o923 = new Object();
        Object o924 = new Object();
        Object o925 = new Object();
        Object o926 = new Object();
        Object o927 = new Object();
        Object o928 = new Object();
        Object o929 = new Object();
        Object o930 = new Object();
        Object o931 = new Object();
        Object o932 = new Object();
        Object o933 = new Object();
        Object o934 = new Object();
        Object o935 = new Object();
        Object o936 = new Object();
        Object o937 = new Object();
        Object o938 = new Object();
        Object o939 = new Object();
        Object o940 = new Object();
        Object o941 = new Object();
        Object o942 = new Object();
        Object o943 = new Object();
        Object o944 = new Object();
        Object o945 = new Object();
        Object o946 = new Object();
        Object o947 = new Object();
        Object o948 = new Object();
        Object o949 = new Object();
        Object o950 = new Object();
        Object o951 = new Object();
        Object o952 = new Object();
        Object o953 = new Object();
        Object o954 = new Object();
        Object o955 = new Object();
        Object o956 = new Object();
        Object o957 = new Object();
        Object o958 = new Object();
        Object o959 = new Object();
        Object o960 = new Object();
        Object o961 = new Object();
        Object o962 = new Object();
        Object o963 = new Object();
        Object o964 = new Object();
        Object o965 = new Object();
        Object o966 = new Object();
        Object o967 = new Object();
        Object o968 = new Object();
        Object o969 = new Object();
        Object o970 = new Object();
        Object o971 = new Object();
        Object o972 = new Object();
        Object o973 = new Object();
        Object o974 = new Object();
        Object o975 = new Object();
        Object o976 = new Object();
        Object o977 = new Object();
        Object o978 = new Object();
        Object o979 = new Object();
        Object o980 = new Object();
        Object o981 = new Object();
        Object o982 = new Object();
        Object o983 = new Object();
        Object o984 = new Object();
        Object o985 = new Object();
        Object o986 = new Object();
        Object o987 = new Object();
        Object o988 = new Object();
        Object o989 = new Object();
        Object o990 = new Object();
        Object o991 = new Object();
        Object o992 = new Object();
        Object o993 = new Object();
        Object o994 = new Object();
        Object o995 = new Object();
        Object o996 = new Object();
        Object o997 = new Object();
        Object o998 = new Object();
        Object o999 = new Object();
    }

    static class Deep0 {}
    static class Deep1 extends Deep0 {}
    static class Deep2 extends Deep1 {}
    static class Deep3 extends Deep2 {}
    static class Deep4 extends Deep3 {}
    static class Deep5 extends Deep4 {}
    static class Deep6 extends Deep5 {}
    static class Deep7 extends Deep6 {}
    static class Deep8 extends Deep7 {}
    static class Deep9 extends Deep8 {}
    static class Deep10 extends Deep9 {}
    static class Deep11 extends Deep10 {}
    static class Deep12 extends Deep11 {}
    static class Deep13 extends Deep12 {}
    static class Deep14 extends Deep13 {}
    static class Deep15 extends Deep14 {}
    static class Deep16 extends Deep15 {}
    static class Deep17 extends Deep16 {}
    static class Deep18 extends Deep17 {}
    static class Deep19 extends Deep18 {}
    static class Deep20 extends Deep19 {}
    static class Deep21 extends Deep20 {}
    static class Deep22 extends Deep21 {}
    static class Deep23 extends Deep22 {}
    static class Deep24 extends Deep23 {}
    static class Deep25 extends Deep24 {}
    static class Deep26 extends Deep25 {}
    static class Deep27 extends Deep26 {}
    static class Deep28 extends Deep27 {}
    static class Deep29 extends Deep28 {}
    static class Deep30 extends Deep29 {}
    static class Deep31 extends Deep30 {}
    static class Deep32 extends Deep31 {}
    static class Deep33 extends Deep32 {}
    static class Deep34 extends Deep33 {}
    static class Deep35 extends Deep34 {}
    static class Deep36 extends Deep35 {}
    static class Deep37 extends Deep36 {}
    static class Deep38 extends Deep37 {}
    static class Deep39 extends Deep38 {}
    static class Deep40 extends Deep39 {}
    static class Deep41 extends Deep40 {}
    static class Deep42 extends Deep41 {}
    static class Deep43 extends Deep42 {}
    static class Deep44 extends Deep43 {}
    static class Deep45 extends Deep44 {}
    static class Deep46 extends Deep45 {}
    static class Deep47 extends Deep46 {}
    static class Deep48 extends Deep47 {}
    static class Deep49 extends Deep48 {}
    static class Deep50 extends Deep49 {}
    static class Deep51 extends Deep50 {}
    static class Deep52 extends Deep51 {}
    static class Deep53 extends Deep52 {}
    static class Deep54 extends Deep53 {}
    static class Deep55 extends Deep54 {}
    static class Deep56 extends Deep55 {}
    static class Deep57 extends Deep56 {}
    static class Deep58 extends Deep57 {}
    static class Deep59 extends Deep58 {}
    static class Deep60 extends Deep59 {}
    static class Deep61 extends Deep60 {}
    static class Deep62 extends Deep61 {}
    static class Deep63 extends Deep62 {}
    static class Deep64 extends Deep63 {}
    static class Deep65 extends Deep64 {}
    static class Deep66 extends Deep65 {}
    static class Deep67 extends Deep66 {}
    static class Deep68 extends Deep67 {}
    static class Deep69 extends Deep68 {}
    static class Deep70 extends Deep69 {}
    static class Deep71 extends Deep70 {}
    static class Deep72 extends Deep71 {}
    static class Deep73 extends Deep72 {}
    static class Deep74 extends Deep73 {}
    static class Deep75 extends Deep74 {}
    static class Deep76 extends Deep75 {}
    static class Deep77 extends Deep76 {}
    static class Deep78 extends Deep77 {}
    static class Deep79 extends Deep78 {}
    static class Deep80 extends Deep79 {}
    static class Deep81 extends Deep80 {}
    static class Deep82 extends Deep81 {}
    static class Deep83 extends Deep82 {}
    static class Deep84 extends Deep83 {}
    static class Deep85 extends Deep84 {}
    static class Deep86 extends Deep85 {}
    static class Deep87 extends Deep86 {}
    static class Deep88 extends Deep87 {}
    static class Deep89 extends Deep88 {}
    static class Deep90 extends Deep89 {}
    static class Deep91 extends Deep90 {}
    static class Deep92 extends Deep91 {}
    static class Deep93 extends Deep92 {}
    static class Deep94 extends Deep93 {}
    static class Deep95 extends Deep94 {}
    static class Deep96 extends Deep95 {}
    static class Deep97 extends Deep96 {}
    static class Deep98 extends Deep97 {}
    static class Deep99 extends Deep98 {}
    static class Deep100 extends Deep99 {}

    static class DeepCloneable extends Deep100 implements Cloneable {
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    public void time_Object_clone(int reps) {
        try {
            CloneableObject o = new CloneableObject();
            for (int rep = 0; rep < reps; ++rep) {
                o.clone();
            }
        } catch (Exception e) {
            throw new AssertionError(e.getMessage());
        }
    }

    public void time_Object_manyFieldClone(int reps) {
        try {
            CloneableManyFieldObject o = new CloneableManyFieldObject();
            for (int rep = 0; rep < reps; ++rep) {
                o.clone();
            }
        } catch (Exception e) {
            throw new AssertionError(e.getMessage());
        }
    }

    public void time_Object_deepClone(int reps) {
        try {
            DeepCloneable o = new DeepCloneable();
            for (int rep = 0; rep < reps; ++rep) {
                o.clone();
            }
        } catch (Exception e) {
            throw new AssertionError(e.getMessage());
        }
    }

    public void time_Array_clone(int reps) {
        int[] o = new int[32];
        for (int rep = 0; rep < reps; ++rep) {
            o.clone();
        }
    }

    public void time_ObjectArray_smallClone(int reps) {
        Object[] o = new Object[32];
        for (int i = 0; i < o.length / 2; ++i) {
            o[i] = new Object();
        }
        for (int rep = 0; rep < reps; ++rep) {
            o.clone();
        }
    }

    public void time_ObjectArray_largeClone(int reps) {
        Object[] o = new Object[2048];
        for (int i = 0; i < o.length / 2; ++i) {
            o[i] = new Object();
        }
        for (int rep = 0; rep < reps; ++rep) {
            o.clone();
        }
    }
}
