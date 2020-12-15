import matplotlib.pyplot as plt
import csv

if (__name__ == '__main__'):
    y1 = []
    y2 = []

    with open('/Users/Chris/Documents/conferences/ecoop2021/empirical-evaluation/response-time-control.csv','r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        next(plots)
        for row in plots:
            y1.append(int(row[1]))

    with open('/Users/Chris/Documents/conferences/ecoop2021/empirical-evaluation/response-time-monitored.csv','r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        next(plots)
        for row in plots:
            y2.append(int(row[1]))


    n = 1
    x = range(0,len(y1)/n)
    y1avg = [sum(y1[i:i+n])//n for i in range(0,len(y1),n)]
    y2avg = [sum(y2[i:i+n])//n for i in range(0,len(y2),n)]
    print(len(y1avg))
    print(len(y2avg))

    plt.plot(x,y1avg, label='control')
    plt.plot(x,y2avg, label='monitored')
    plt.xlabel('Request')
    plt.ylabel('Response Time')
    plt.title('Response time graph')
    plt.legend()
    plt.show()