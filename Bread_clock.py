principle_for7493_IC  = ["A","BCD"]
# 將4bits ripple carry adder 串接起來(A : BCD)
MOD10 = "為什麼Qa 要串到 B 。 因為要達成ripple效果 ， 所以要加前一個output 變成下一個clock的input"
__int10__ = "(1010)2 ==> Qd Qb 串到R1 R2 做reset ，而7493本身IC會有and gate 做判斷"
#    ==> 所以不用另外外接and gate 做reset

MOD6 = "MOD6 ==> 兩種接法"
method1 = "MOD6 = Qa Qb Qc Qd ==> [0110]"
# 照原本的接法 ==> MOD6 = Qa Qb Qc Qd ==> 0110
method2 = "MOD6 = Qb Qc Qd ==> [110]"
# MOD6 = Qb Qc Qd ==> 110


FOR_method2 = {"7493":"7447",Qb:A ,Qc:B ,Qd:C} ==> 對於他的input A跟Qa 就不用接任何東西
# 因為只用BCD 也就是下半部分的counter
For7447_IC  = "D input must be low" 因為會預設是高電位
__not_low_forD = "每次都是從8開始加"

MOD60 = [MOD6 , MOD10]
# for method2 mod10 的Qd 要接到mod6 的B 當作clock   

MOD24 = [MOD3,MOD10]
對於個位數判斷 ==> 兩種
reset = "歸零"
__for1__ = {10 ,reset}
__for2__ = {(2,4) ,reset}

==> 個位數需要and 跟or gate 做判斷
one_output = "當(2,4) and" 給十位數跟個位數
result = f"or for {one_output} + {10}" 個位數