S_game=rec X.(+{!Guess(num: Int)[0.75,*].&{?Correct()[0.01].X, ?Incorrect()[*].X},
                !Help()[*,0.2].?Hint(info: String)[*].X,
                !Quit()[*].end})