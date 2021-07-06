S_game=rec X.(
	&{?Guess(num: Int)[num > 1 && num < 100].+{
		!Correct(ans: Int)[ans==num],
		!Incorrect().X
	},
	?Quit()
})