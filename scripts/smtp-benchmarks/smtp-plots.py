import csv
import matplotlib.pyplot as plt
import statistics
import sys


def extract_exec_time_info(path):
    with open(path, 'r') as csvfile:
        rows = csv.reader(csvfile, delimiter=',')
        start_time = next(rows)[0]
        time = []
        for row in rows:
            time.append(int(row[2]))
        return start_time, time


def extract_cpu_mem_info(path):
    with open(path, 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',', skipinitialspace=True)
        cpu = []
        mem = []
        for row in plots:
            cpu.append(int(row[0].strip("%")))
            mem.append(int(row[1]))
        return cpu, mem


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
        tmp_start_time, tmp_time = extract_exec_time_info(f'{path}/{requests}_exec_time_run{i}.csv')
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


def plot(x, y1, y2, y3, y1err, y2err, y3err, y1_label, y2_label, y3_label, ylabel, xlabel, title, path, type):
    fig, ax1 = plt.subplots()

    plt.grid(linestyle=':', linewidth=0.8)
    ax1.tick_params(axis="y", direction="in")
    ax1.tick_params(axis="x", direction="in")

    lns1 = ax1.plot(x, y1, label=f'{y1_label}', linestyle="solid", linewidth=1, marker="s", markersize=2,
                    markeredgewidth=1)
    if(type=="resp_time"):
        lns1 += ax1.plot(x, y3, label=f'{y3_label}', linestyle="dashed", linewidth=1, marker="x", markersize=3, markeredgewidth=0.8)
    lns1 += ax1.plot(x, y2, label=f'{y2_label}', linestyle="dotted", linewidth=1, color="C2", marker=".", markersize=4, markeredgewidth=0.8)

    # lns1 = ax1.errorbar(x, y1, label=f'{y1_label}', linestyle="solid", linewidth=1, marker="s", markersize=2, markeredgewidth=1, yerr=y1err, fmt='-')
    # lns1 += ax1.errorbar(x, y2, label=f'{y2_label}', linestyle="dashed", linewidth=1, marker="x", markersize=3, markeredgewidth=0.8, yerr=y2err, fmt='-')
    # lns1 += ax1.errorbar(x, y3, label=f'{y3_label}', linestyle="dotted", linewidth=1, marker=".", markersize=4, markeredgewidth=0.8, yerr=y3err, fmt='-')
    # lns1 += ax1.errorbar(x, y4, label=f'{y4_label}', linestyle=(0, (3, 5, 1, 5)), linewidth=1, marker="d", markersize=2, markeredgewidth=1, yerr=y4err, fmt='-')

    # ax1.legend()
    # frame = ax1.legend(bbox_to_anchor=(0, -0.4), loc='lower left', ncol=4, borderpad=0.4).get_frame()
    # frame.set_linewidth(0.5)

    # legend_elements = [Line2D([0], [0], color="C1", ls="dashed", lw=1, label=f'{y4_label}', marker="x", markersize=3, markeredgewidth=0.8)]

    # frame = ax1.legend(handles=legend_elements, bbox_to_anchor=(0, 1.23), loc='upper left', ncol=4, borderpad=0.4).get_frame()
    # frame.set_linewidth(0.5)

    ax1.set_xlabel(f'{xlabel}')
    ax1.set_ylabel(f'{ylabel}')
    ax1.legend()
    plt.title(f'{title}')

    plt.savefig(f'{path}.pdf', dpi=300)
    # plt.show()

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

    detached_mon_times = []
    detached_mon_total_times = []
    detached_mon_resp_times = []
    detached_mon_cpus = []
    detached_mon_mems = []

    detached_mon_total_times_variance = []
    detached_mon_resp_times_variance = []
    detached_mon_cpus_variance = []
    detached_mon_mems_variance = []

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

        detached_mon_times.append(average(detached_mon_collective_avg_time))
        detached_mon_total_times.append(detached_mon_collective_avg_total_time)
        detached_mon_resp_times.append(detached_mon_collective_avg_resp_time)
        detached_mon_cpus.append(detached_mon_collective_avg_cpu)
        detached_mon_mems.append(detached_mon_collective_avg_mem)

        detached_mon_total_times_variance.append(detached_mon_total_time_variance)
        detached_mon_resp_times_variance.append(detached_mon_resp_time_variance)
        detached_mon_cpus_variance.append(detached_mon_cpu_variance)
        detached_mon_mems_variance.append(detached_mon_mem_variance)


    # plot(x, control_times, monitored_times, lchannels_times, "control", "monitored", "lchannels", "Time/ns", "Experiments", "Average Execution Times", "")

    plots_path = path+"/plots/"

    # print("exec times percentage increase unsafe -> monitored",
    #       percentage_inc(average(control_total_times), average(monitored_total_times)))
    # print("exec times percentage increase control -> detached_mon",
    #       percentage_inc(average(control_total_times), average(detached_mon_total_times)))
    # print("exec times percentage increase detached_mon -> monitored",
    #       percentage_inc(average(detached_mon_total_times), average(monitored_total_times)))

    # plot(x, control_total_times, monitored_total_times, detached_mon_total_times,
    #      control_total_times_variance, monitored_total_times_variance, detached_mon_total_times_variance,
    #      "unsafe", "monitored", "detached_mon", "Time (s)", "Emails sent", "Execution Times", path + "smtp_total_times", "total_times")

    print(f"*** SMTP ${type} benchmark overheads:")
    print("cpu percentage increase control -> monitored",
          percentage_inc(average(control_cpus), average(monitored_cpus)))
    print("cpu percentage increase control -> detached_mon",
          percentage_inc(average(control_cpus), average(detached_mon_cpus)))

    plot(x, control_cpus, monitored_cpus, detached_mon_cpus,
         control_cpus_variance, monitored_cpus_variance, detached_mon_cpus_variance,
         "unsafe", "monitored", "detached_mon", "CPU Utilisation (%)", "Emails sent", "CPU Utilisation",
         plots_path + "smtp_cpu_consumption", "cpu_consumption")

    print("memory percentage increase control -> monitored",
          percentage_inc(average(control_mems), average(monitored_mems)))
    print("memory percentage increase control -> detached_mon",
          percentage_inc(average(control_mems), average(detached_mon_mems)))

    plot(x, control_mems, monitored_mems, detached_mon_mems,
         control_mems_variance, monitored_mems_variance, detached_mon_mems_variance,
         "unsafe", "monitored", "detached_mon", "Memory Consumption (MB)", "Emails sent", "Memory Consumption",
         plots_path + "smtp_mem_consumption", "memory_consumption")


    plot(x, control_resp_times, monitored_resp_times, detached_mon_resp_times,
         control_resp_times_variance, monitored_resp_times_variance, detached_mon_resp_times_variance,
         "unsafe", "monitored", "detached_mon", "Response Time (ms)", "Emails sent", "Response Times",
         plots_path + "smtp_resp_time", "resp_time")

    print("resp times percentage increase control -> monitored",
          percentage_inc(average(control_resp_times), average(monitored_resp_times)))
    print("resp times percentage increase control -> detached_mon",
          percentage_inc(average(control_resp_times), average(detached_mon_resp_times)))
