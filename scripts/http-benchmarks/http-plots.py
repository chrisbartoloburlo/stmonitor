import csv
import matplotlib.pyplot as plt
import statistics
import sys

def extract_resp_time_info(path):
    with open(path, 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',')
        next(plots)
        time = []
        err = []
        for row in plots:
            time.append(int(row[1]))
            if (row[4] != 'OK'):
                err.append(int(1))
            else:
                err.append(int(0))
        return time, err


def extract_cpu_mem_info(path, requests):
    with open(path, 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',', skipinitialspace=True)
        next(plots)
        cpu = []
        mem = []
        for row in plots:
            try:
                cpu.append(int(row[0].strip("%")))
                mem.append(float(row[1]))
            except:
                None
        return cpu, mem



def extract_exec_time_info(path):
    with open(path, 'r') as csvfile:
        plots = csv.reader(csvfile, delimiter=',', skipinitialspace=True)
        next(plots)
        exectime = []
        for row in plots:
            exectime.append(int(row[1]))
        return exectime

def collective_res(path, runs, requests):
    collective_time = []
    collective_err = []
    for i in range(1, runs + 1):
        tmp_time, tmp_err = extract_resp_time_info(f'{path}/{requests}_resp_time_run{i}.csv')
        collective_time.append(tmp_time)
        collective_err.append(tmp_err)

    tmp_cpu, tmp_mem = extract_cpu_mem_info(f'{path}/{requests}_cpu_mem.txt', requests)
    # tmp_time = extract_exec_time_info(f'{path}/{requests}_exec_times.txt')
    return collective_time, collective_err, tmp_cpu, convert_kb_mb(tmp_mem), tmp_time


def get_average(lst, runs):
    return [sum(x) / runs for x in zip(*lst)]


def convert_kb_mb(lst):
    return [i/1000 for i in lst]


def individual_experiment(path, runs, requests):
    collective_time, collective_err, collective_cpu, collective_mem, collective_exec_times = collective_res(path, runs, requests)
    collective_avg_time = get_average(collective_time, runs)
    collective_avg_err = get_average(collective_err, runs)
    collective_avg_cpu = average(collective_cpu)
    collective_avg_mem = average(collective_mem)

    collective_avg_exec_times = average(collective_exec_times)
    return collective_avg_time, collective_avg_err, collective_avg_cpu, collective_avg_mem, collective_avg_exec_times

def plot_response_time_err_individual(x, plot_err, control_time, monitored_time, control_err, monitored_err, title):
    fig, ax1 = plt.subplots()

    lns1 = ax1.plot(x, control_time, label='time_control')
    lns1 += ax1.plot(x, monitored_time, label='time_monitored')

    if plot_err:
        ax2 = ax1.twinx()
        lns2 = ax2.plot(x, control_err, color='C4', label='err_control')
        lns2 += ax2.plot(x, monitored_err, color='C5', label='err_monitored')

        leg = lns1 + lns2
        labs = [l.get_label() for l in leg]
        ax1.legend(leg, labs, loc=1)
        ax2.set_ylabel('Error Rate')
    else:
        ax1.legend()

    ax1.set_xlabel('Experiment')
    ax1.set_ylabel('Response Time')
    plt.title(f'{title} Response time graph')
    plt.show()


def plot(x, y1, y2, y1_label, y2_label, ylabel, xlabel, title, path):
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
    #     # 'text.latex.preview': True
    # })

    fig, ax1 = plt.subplots()

    plt.grid(linestyle=':', linewidth=0.8)

    ax1.tick_params(axis="y", direction="in")
    ax1.tick_params(axis="x", direction="in")

    lns1 = ax1.plot(x, y1, label=f'{y1_label}', linestyle="solid", linewidth=1, color="C0", marker="s", markersize=2, markeredgewidth=1)
    lns1 += ax1.plot(x, y2, label=f'{y2_label}', linestyle="dotted", linewidth=1, color="C2", marker=".", markersize=4, markeredgewidth=0.8)

    ax1.set_xlabel(f'{xlabel}')
    ax1.set_ylabel(f'{ylabel}')
    ax1.legend()
    plt.title(f'{title}')

    plt.savefig(f'{path}.pdf', dpi=300)
    # plt.show()


def plot_cpu_mem_individual(cpu, cpu_mem_sizes, control_cpu_mem, monitored_cpu_mem, requests):
    x_2 = range(0, min(cpu_mem_sizes))
    fig, ax = plt.subplots()

    ax.plot(x_2, control_cpu_mem[:len(x_2)], label='control')
    ax.plot(x_2, monitored_cpu_mem[:len(x_2)], label='monitored')

    ax.set_xlabel('Timestamp')
    if cpu:
        ax.set_ylabel(f'%CPU')
        plt.title(f'CPU Consumption for {requests} requests')
    else:
        ax.set_ylabel('%Memory')
        plt.title(f'Memory Consumption for {requests} requests')
    ax.legend()
    plt.show()


def average(lst):
    return sum(lst) / len(lst)


def percentage_inc(original, new):
    inc = new - original
    return (inc / original) * 100


if __name__ == '__main__':
    plt.style.use('seaborn-deep')

    path = sys.argv[1]+'/scripts/http-benchmarks'
    runs = int(sys.argv[2])
    kickthetires = int(sys.argv[3])

    control_resp_times = []
    control_errs = []
    control_cpus = []
    control_mems = []
    control_exec_times = []

    monitored_resp_times = []
    monitored_errs = []
    monitored_cpus = []
    monitored_mems = []
    monitored_exec_times = []

    if(kickthetires==1):
        x = [200,600]
    else:
        x = range(200, 2001, 200)

    for iterations in x:
        control_collective_avg_time, control_collective_avg_err, control_collective_avg_cpu, control_collective_avg_mem, control_collective_avg_total_time = individual_experiment(path+"/results/control", runs, iterations)
        monitored_collective_avg_time, monitored_collective_avg_err, monitored_collective_avg_cpu, monitored_collective_avg_mem, monitored_avg_total_time = individual_experiment(path+"/results/monitored", runs, iterations)

        control_resp_times.append(average(control_collective_avg_time))
        control_errs.append(average(control_collective_avg_err))
        control_cpus.append(control_collective_avg_cpu)
        control_mems.append(control_collective_avg_mem)
        control_exec_times.append(control_collective_avg_total_time)

        monitored_resp_times.append(average(monitored_collective_avg_time))
        monitored_errs.append(average(monitored_collective_avg_err))
        monitored_cpus.append(monitored_collective_avg_cpu)
        monitored_mems.append(monitored_collective_avg_mem)
        monitored_exec_times.append(monitored_avg_total_time)

    plots_path = path+"/plots/"

    print(f"*** HTTP benchmark overheads:")
    print("cpu percentage increase control -> monitored", percentage_inc(average(control_cpus), average(monitored_cpus)))
    plot(x, control_cpus, monitored_cpus,
         "control", "monitored", "CPU Utilisation (%)", "Requests sent", "CPU Utilisation", plots_path+"cpu_consumption")

    print("memory percentage increase control -> monitored", percentage_inc(average(control_mems), average(monitored_mems)))
    plot(x, control_mems, monitored_mems,
         "unsafe", "monitored", "Memory Consumption (MB)", "Requests sent", "Memory Consumption", plots_path+"mem_consumption")

    print("resp times percentage increase control -> monitored", percentage_inc(average(control_resp_times), average(monitored_resp_times)))
    plot(x, control_resp_times, monitored_resp_times,
         "unsafe", "monitored", "Response Time (ms)", "Requests sent", "Response Times", plots_path+"resp_time")