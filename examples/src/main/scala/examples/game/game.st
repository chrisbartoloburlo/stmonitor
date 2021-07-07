S_game=rec X.(+{!Guess(num: Int)[0.75].&{?Correct()[0.01].X, ?Incorrect()[0.99].X},
                !Help()[0.2].?Hint(info: String)[1].X,
                !Quit()[0.05].end})