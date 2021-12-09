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
    plt.plot(x, y3, label=f'{y3_label}', linestyle="dashed", linewidth=1)

    plt.xlabel(f'{xlabel}')
    plt.ylabel(f'{ylabel}')
    # plt.legend(loc='upper right')
    # plt.title(f'{title}')


def animate(i):
    min_boundaries, max_boundaries, prob = extract_info(path)
    plot(range(1, len(min_boundaries)+1, 1), min_boundaries, max_boundaries, prob, "Confidence Interval", "Confidence Interval", "Estimated Probability", "Probability", "Iterations", title, path+f'{title}')


live = int(sys.argv[1])
path = sys.argv[2]
title = sys.argv[3]

if __name__ == '__main__':
    plt.style.use('seaborn-deep')

    if(live==0):
        # plt.rcParams.update({
        #     "figure.figsize": (2.8, 2),
        #     "pgf.texsystem": "pdflatex",
        #     'font.family': 'serif',
        #     'font.size': 8.5,
        #     'text.usetex': True,
        #     'pgf.rcfonts': False,
        #     'legend.frameon': True,
        #     'legend.framealpha': 1.0,
        #     'legend.edgecolor': 'black',
        #     'legend.fancybox': False,
        #     'legend.shadow': False,
        #     'axes.linewidth': 0.5,
        #     'axes.unicode_minus': False
        # })
        min_boundaries, max_boundaries, prob = extract_info(path)
        plot(range(1, len(min_boundaries)+1, 1), min_boundaries, max_boundaries, prob, "Confidence Interval", "Confidence Interval", "Estimated Probability", "Probability", "Iterations", title, path+f'{title}')

        plt.legend()
        # frame = plt.legend(bbox_to_anchor=(0, 1.23), loc='upper left', ncol=2, borderpad=0.4).get_frame()
        # frame.set_linewidth(0.5)

        save_path = sys.argv[4]
        plt.savefig(save_path+f'/{title}.pdf', dpi=1500, bbox_inches='tight', pad_inches=0.1)
        # plt.show()
    elif(live==1):
        ani = FuncAnimation(plt.gcf(), animate, interval=100, repeat=False)
        plt.show()

