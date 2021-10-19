import matplotlib.pyplot as plt
import pandas as pd
from matplotlib.animation import FuncAnimation
import sys

def extract_info(path):
    data = pd.read_csv(path)
    min_boundaries = data['pmin']
    max_boundaries = data['pmax']
    prob = data['pe']
    return min_boundaries, max_boundaries, prob

def plot(x, y1, y2, y3, y1_label, y2_label, y3_label, ylabel, xlabel, title, path):
    plt.cla()

    plt.grid(linestyle=':', linewidth=0.8)
    plt.tick_params(axis="y", direction="in")
    plt.tick_params(axis="x", direction="in")

    # color = next(plt._get_lines.prop_cycler)['color']
    plt.plot(x, y1, label=f'{y1_label}', linestyle="solid", linewidth=1,color='C2')
    plt.plot(x, y2, linestyle="solid", linewidth=1,color='C2')
    plt.plot(x, y3, label=f'{y3_label}', linestyle="solid", linewidth=1)

    plt.xlabel(f'{xlabel}')
    plt.ylabel(f'{ylabel}')
    plt.legend(loc='upper right')
    plt.title(f'{title}')
    # plt.savefig(f'{path}.pdf', dpi=300)


def animate(i):
    min_boundaries, max_boundaries, prob = extract_info(path)
    plot(range(1, len(min_boundaries)+1, 1), min_boundaries, max_boundaries, prob, "Boundary", "Boundary", "Estimated Probability", "Probability", "Iterations", title, path+f'{title}')


live = int(sys.argv[1])
path = sys.argv[2]
title = sys.argv[3]

if __name__ == '__main__':
    plt.style.use('seaborn-deep')

    if(live==0):
        min_boundaries, max_boundaries, prob = extract_info(path)
        plot(range(1, len(min_boundaries)+1, 1), min_boundaries, max_boundaries, prob, "Boundary", "Boundary", "Estimated Probability", "Probability", "Iterations", title, path+f'{title}')
    elif(live==1):
        ani = FuncAnimation(plt.gcf(), animate, interval=100, repeat=False)
        plt.show()







