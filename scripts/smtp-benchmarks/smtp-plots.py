import csv
import matplotlib.pyplot as plt
import pandas as pd
import numpy as np
import statistics
import sys


def extract_exec_time_info(path):
    data = pd.read_csv(path)
    return data.iloc[:, 2].astype(int)


def extract_cpu_mem_info(path):
    data = pd.read_csv(path)
    return data.iloc[:, 0].str.rstrip('%').astype(int), data.iloc[:, 1].astype(int)


def get_average(lst, runs):
    return [sum(x) / runs for x in zip(*lst)]


def average(lst):
    return sum(lst) / len(lst)


def percentage_inc(original, new):
    inc = new - original
    return (inc / original) * 100


def collective_res(path, runs, requests):
    collective_time = []
    collective_total_time = []
    collective_response_time = []
    requestsinloop = 4
    for i in range(1, runs + 1):
        tmp_time = extract_exec_time_info(f'{path}/{requests}_exec_time_run{i}.csv')
        collective_time.append(tmp_time)
        collective_total_time.append(sum(tmp_time))
        collective_response_time.append(average(tmp_time) / requestsinloop)

    tmp_cpu, tmp_mem = extract_cpu_mem_info(f'{path}/{requests}_cpu_mem_run.txt')
    return collective_time, collective_total_time, collective_response_time, tmp_cpu, convert_kb_mb(tmp_mem)


def individual_experiment(path, runs, requests):
    collective_time, collective_total_time, collective_response_time, collective_cpu, collective_mem = collective_res(
        path, runs, requests)
    collective_avg_time = get_average(collective_time, runs)
    collective_avg_total_time = average(convert_ns_s(collective_total_time))

    collective_avg_response_time = average(convert_ns_ms(collective_response_time))
    collective_avg_cpu = average(collective_cpu)
    collective_avg_mem = average(collective_mem)

    total_time_variance = statistics.pvariance(convert_ns_s(collective_total_time))
    response_time_variance = statistics.pvariance(convert_ns_ms(collective_response_time))
    cpu_variance = statistics.pvariance(collective_cpu)
    mem_variance = statistics.pvariance(collective_mem)

    return collective_avg_time, collective_avg_total_time, collective_avg_response_time, collective_avg_cpu, collective_avg_mem, total_time_variance, response_time_variance, cpu_variance, mem_variance


def plot(x, y1, y2, y3, y2_oh, y3_oh, y1err, y2err, y3err, y1_label, y2_label, y3_label, y2_oh_label, y3_oh_label, ylabel, xlabel, title, path, type):
    plt.rcParams.update({
        "figure.figsize": (2.8, 2),
        "pgf.texsystem": "pdflatex",
        'font.family': 'serif',
        'font.size': 8.5,
        'text.usetex': True,
        'pgf.rcfonts': False,
        'legend.frameon': True,
        'legend.framealpha': 1.0,
        'legend.edgecolor': 'black',
        'legend.fancybox': False,
        'legend.shadow': False,
        'axes.linewidth': 0.5,
        # 'text.latex.preview': True
    })

    fig, ax1 = plt.subplots()

    plt.grid(linestyle=':', linewidth=0.8)
    ax1.tick_params(axis="y", direction="in")
    ax1.tick_params(axis="x", direction="in")

    lns1 = ax1.plot(x, y1, label=f'{y1_label}', linestyle="solid", linewidth=1, marker="s", markersize=2, markeredgewidth=1)

    lns1 += ax1.plot(x, y3, label=f'{y3_label}', linestyle="dashed", linewidth=1, color="C1", marker="x", markersize=3, markeredgewidth=0.8)
    lns1 += ax1.plot(x, y3_oh, label=f'{y3_oh_label}', linestyle="dashed", linewidth=1, color="C1", marker="^", markersize=3, markeredgewidth=0.8)
    ax1.fill_between(x, y3, y3_oh, color='C1', alpha=0.3)

    lns1 += ax1.plot(x, y2, label=f'{y2_label}', linestyle="dotted", linewidth=1, color="C2", marker=".", markersize=4, markeredgewidth=0.8)
    lns1 += ax1.plot(x, y2_oh, label=f'{y2_oh_label}', linestyle="dotted", linewidth=1, color="C2", marker="^", markersize=3, markeredgewidth=0.8)
    ax1.fill_between(x, y2, y2_oh, color='C2', alpha=0.3)
    
    ax1.set_xlabel(f'{xlabel}')
    ax1.set_ylabel(f'{ylabel}')

    if(True):
        # ax1.legend()
        # frame = ax1.legend(bbox_to_anchor=(0, 1.23), loc='upper left', ncol=5, borderpad=0.4).get_frame()
        # frame.set_linewidth(0.5)

        legend_elements = []
        legend_elements += [plt.Line2D([0], [0], ls="solid", lw=1, label=f'{y1_label}', marker="s", markersize=2, markeredgewidth=1)]

        legend_elements += [plt.Line2D([0], [0], color="C1", ls="dashed", lw=1, label=f'{y3_label}', marker="x", markersize=3, markeredgewidth=0.8)]
        legend_elements += [plt.Line2D([0], [0], color="C1", ls="dashed", lw=1, label=f'{y3_oh_label}', marker="^", markersize=3, markeredgewidth=0.8)]

        legend_elements += [plt.Line2D([0], [0], color="C2", ls="dotted", lw=1, label=f'{y2_label}', marker=".", markersize=4, markeredgewidth=0.8)]
        legend_elements += [plt.Line2D([0], [0], color="C2", ls="dotted", lw=1, label=f'{y2_oh_label}', marker="^", markersize=3, markeredgewidth=0.8)]

        frame = ax1.legend(handles=legend_elements, bbox_to_anchor=(0, 1.23), loc='upper left', ncol=5, borderpad=0.4).get_frame()
        frame.set_linewidth(0.5)
        plt.savefig(f'{path}.pdf', dpi=500, bbox_inches='tight',pad_inches=1)
    else:
        ax1.legend()
        # plt.title(f'{title}')
        plt.savefig(f'{path}.pdf', dpi=300)

def convert_ns_s(lst):
    return [i / 1000000000 for i in lst]


def convert_ns_ms(lst):
    return [i / 1000000 for i in lst]


def convert_kb_mb(lst):
    return [i / 1000 for i in lst]


if __name__ == '__main__':
    plt.style.use('seaborn-deep')

    path = sys.argv[1]+'/scripts/smtp-benchmarks'
    runs = int(sys.argv[2])
    kickthetires = int(sys.argv[3])
    type = sys.argv[4]

    if(type == "smtp-python"):
        path=path+'/smtp-python'
    else:
        path=path+'/smtp-postfix'

    print(f"*** SMTP {type} benchmark overheads:")

    control_times = []
    control_total_times = []
    control_resp_times = []
    control_cpus = []
    control_mems = []

    control_total_times_variance = []
    control_resp_times_variance = []
    control_cpus_variance = []
    control_mems_variance = []

    monitored_times = []
    monitored_total_times = []
    monitored_resp_times = []
    monitored_cpus = []
    monitored_mems = []

    monitored_total_times_variance = []
    monitored_resp_times_variance = []
    monitored_cpus_variance = []
    monitored_mems_variance = []

    monitored_logging_times = []
    monitored_logging_total_times = []
    monitored_logging_resp_times = []
    monitored_logging_cpus = []
    monitored_logging_mems = []

    monitored_logging_total_times_variance = []
    monitored_logging_resp_times_variance = []
    monitored_logging_cpus_variance = []
    monitored_logging_mems_variance = []

    detached_mon_times = []
    detached_mon_total_times = []
    detached_mon_resp_times = []
    detached_mon_cpus = []
    detached_mon_mems = []

    detached_mon_total_times_variance = []
    detached_mon_resp_times_variance = []
    detached_mon_cpus_variance = []
    detached_mon_mems_variance = []

    detached_mon_logging_times = []
    detached_mon_logging_total_times = []
    detached_mon_logging_resp_times = []
    detached_mon_logging_cpus = []
    detached_mon_logging_mems = []

    detached_mon_logging_total_times_variance = []
    detached_mon_logging_resp_times_variance = []
    detached_mon_logging_cpus_variance = []
    detached_mon_logging_mems_variance = []

    if(kickthetires==1):
        x = [200,600,1000]
    else:
        x = range(200, 2001, 200)

    for iterations in x:
        control_collective_avg_time, control_collective_avg_total_time, control_collective_avg_resp_time, control_collective_avg_cpu, control_collective_avg_mem, control_total_time_variance, control_resp_time_variance, control_cpu_variance, control_mem_variance = individual_experiment(
            path+"/results/control", runs, iterations)
        monitored_collective_avg_time, monitored_collective_avg_total_time, monitored_collective_avg_resp_time, monitored_collective_avg_cpu, monitored_collective_avg_mem, monitored_total_time_variance, monitored_resp_time_variance, monitored_cpu_variance, monitored_mem_variance = individual_experiment(
            path+"/results/monitored", runs, iterations)
        detached_mon_collective_avg_time, detached_mon_collective_avg_total_time, detached_mon_collective_avg_resp_time, detached_mon_collective_avg_cpu, detached_mon_collective_avg_mem, detached_mon_total_time_variance, detached_mon_resp_time_variance, detached_mon_cpu_variance, detached_mon_mem_variance = individual_experiment(
            path+"/results/detached_monitored", runs, iterations)
        
        monitored_logging_collective_avg_time, monitored_logging_collective_avg_total_time, monitored_logging_collective_avg_resp_time, monitored_logging_collective_avg_cpu, monitored_logging_collective_avg_mem, monitored_logging_total_time_variance, monitored_logging_resp_time_variance, monitored_logging_cpu_variance, monitored_logging_mem_variance = individual_experiment(
            path+"/results/monitored_logging", runs, iterations)
        detached_mon_logging_collective_avg_time, detached_mon_logging_collective_avg_total_time, detached_mon_logging_collective_avg_resp_time, detached_mon_logging_collective_avg_cpu, detached_mon_logging_collective_avg_mem, detached_mon_logging_total_time_variance, detached_mon_logging_resp_time_variance, detached_mon_logging_cpu_variance, detached_mon_logging_mem_variance = individual_experiment(
            path+"/results/detached_monitored_logging", runs, iterations)

        control_times.append(average(control_collective_avg_time))
        control_total_times.append(control_collective_avg_total_time)
        control_resp_times.append(control_collective_avg_resp_time)
        control_cpus.append(control_collective_avg_cpu)
        control_mems.append(control_collective_avg_mem)

        control_total_times_variance.append(control_total_time_variance)
        control_resp_times_variance.append(control_resp_time_variance)
        control_cpus_variance.append(control_cpu_variance)
        control_mems_variance.append(control_mem_variance)

        monitored_times.append(average(monitored_collective_avg_time))
        monitored_total_times.append(monitored_collective_avg_total_time)
        monitored_resp_times.append(monitored_collective_avg_resp_time)
        monitored_cpus.append(monitored_collective_avg_cpu)
        monitored_mems.append(monitored_collective_avg_mem)

        monitored_total_times_variance.append(monitored_total_time_variance)
        monitored_resp_times_variance.append(monitored_resp_time_variance)
        monitored_cpus_variance.append(monitored_cpu_variance)
        monitored_mems_variance.append(monitored_mem_variance)

        monitored_logging_times.append(average(monitored_logging_collective_avg_time))
        monitored_logging_total_times.append(monitored_logging_collective_avg_total_time)
        monitored_logging_resp_times.append(monitored_logging_collective_avg_resp_time)
        monitored_logging_cpus.append(monitored_logging_collective_avg_cpu)
        monitored_logging_mems.append(monitored_logging_collective_avg_mem)

        monitored_logging_total_times_variance.append(monitored_logging_total_time_variance)
        monitored_logging_resp_times_variance.append(monitored_logging_resp_time_variance)
        monitored_logging_cpus_variance.append(monitored_logging_cpu_variance)
        monitored_logging_mems_variance.append(monitored_logging_mem_variance)

        detached_mon_times.append(average(detached_mon_collective_avg_time))
        detached_mon_total_times.append(detached_mon_collective_avg_total_time)
        detached_mon_resp_times.append(detached_mon_collective_avg_resp_time)
        detached_mon_cpus.append(detached_mon_collective_avg_cpu)
        detached_mon_mems.append(detached_mon_collective_avg_mem)

        detached_mon_total_times_variance.append(detached_mon_total_time_variance)
        detached_mon_resp_times_variance.append(detached_mon_resp_time_variance)
        detached_mon_cpus_variance.append(detached_mon_cpu_variance)
        detached_mon_mems_variance.append(detached_mon_mem_variance)

        detached_mon_logging_times.append(average(detached_mon_logging_collective_avg_time))
        detached_mon_logging_total_times.append(detached_mon_logging_collective_avg_total_time)
        detached_mon_logging_resp_times.append(detached_mon_logging_collective_avg_resp_time)
        detached_mon_logging_cpus.append(detached_mon_logging_collective_avg_cpu)
        detached_mon_logging_mems.append(detached_mon_logging_collective_avg_mem)

        detached_mon_logging_total_times_variance.append(detached_mon_logging_total_time_variance)
        detached_mon_logging_resp_times_variance.append(detached_mon_logging_resp_time_variance)
        detached_mon_logging_cpus_variance.append(detached_mon_logging_cpu_variance)
        detached_mon_logging_mems_variance.append(detached_mon_logging_mem_variance)

    plots_path = path+"/plots/"

    print("cpu percentage increase control -> grey-box",
          percentage_inc(average(control_cpus), average(monitored_cpus)))
    print("cpu percentage increase control -> black-box",
          percentage_inc(average(control_cpus), average(detached_mon_cpus)))

    print("cpu percentage increase control -> grey-box w/ logging",
          percentage_inc(average(control_cpus), average(monitored_logging_cpus)))
    print("cpu percentage increase control -> black-box w/ logging",
          percentage_inc(average(control_cpus), average(detached_mon_logging_cpus)))


    plot(x, control_cpus, monitored_cpus, detached_mon_cpus, monitored_logging_cpus, detached_mon_logging_cpus,
         control_cpus_variance, monitored_cpus_variance, detached_mon_cpus_variance, 
         "unsafe", "grey-box", "black-box", "grey-box(log)", "black-box(log)", "CPU Utilisation (%)", "Emails sent", "CPU Utilisation",
         plots_path + "smtp_cpu_consumption", "cpu_consumption")

    print("memory percentage increase control -> grey-box",
          percentage_inc(average(control_mems), average(monitored_mems)))
    print("memory percentage increase control -> black-box",
          percentage_inc(average(control_mems), average(detached_mon_mems)))

    print("memory percentage increase control -> grey-box w/ logging",
          percentage_inc(average(control_mems), average(monitored_logging_mems)))
    print("memory percentage increase control -> black-box w/ logging",
          percentage_inc(average(control_mems), average(detached_mon_logging_mems)))

    plot(x, control_mems, monitored_mems, detached_mon_mems, monitored_logging_mems, detached_mon_logging_mems,
         control_mems_variance, monitored_mems_variance, detached_mon_mems_variance,
         "unsafe", "grey-box", "black-box", "grey-box(log)", "black-box(log)", "Memory Consumption (MB)", "Emails sent", "Memory Consumption",
         plots_path + "smtp_mem_consumption", "memory_consumption")


    plot(x, control_resp_times, monitored_resp_times, detached_mon_resp_times, monitored_logging_resp_times, detached_mon_logging_resp_times, 
         control_resp_times_variance, monitored_resp_times_variance, detached_mon_resp_times_variance,
         "unsafe", "grey-box", "black-box", "grey-box(log)", "black-box(log)", "Response Time (ms)", "Emails sent", "Response Times",
         plots_path + "smtp_resp_time", "resp_time")

    print("resp times percentage increase control -> grey-box",
          percentage_inc(average(control_resp_times), average(monitored_resp_times)))
    print("resp times percentage increase control -> black-box",
          percentage_inc(average(control_resp_times), average(detached_mon_resp_times)))

    print("resp times percentage increase control -> grey-box w/ logging",
          percentage_inc(average(control_resp_times), average(monitored_logging_resp_times)))
    print("resp times percentage increase control -> black-box w/ logging",
          percentage_inc(average(control_resp_times), average(detached_mon_logging_resp_times)))

